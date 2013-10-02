package com.continuuity.data.runtime.main;

import com.continuuity.common.conf.CConfiguration;
import com.continuuity.common.conf.Constants;
import com.continuuity.common.conf.KafkaConstants;
import com.continuuity.common.guice.ConfigModule;
import com.continuuity.common.guice.DiscoveryRuntimeModule;
import com.continuuity.common.guice.IOModule;
import com.continuuity.common.guice.LocationRuntimeModule;
import com.continuuity.common.metrics.MetricsCollectionService;
import com.continuuity.common.service.CommandPortService;
import com.continuuity.common.utils.Copyright;
import com.continuuity.data.runtime.DataFabricOpexModule;
import com.continuuity.data2.transaction.distributed.TransactionService;
import com.continuuity.data2.transaction.queue.QueueAdmin;
import com.continuuity.internal.kafka.client.ZKKafkaClientService;
import com.continuuity.kafka.client.KafkaClientService;
import com.continuuity.metrics.guice.MetricsClientRuntimeModule;
import com.continuuity.weave.common.Services;
import com.continuuity.weave.zookeeper.RetryStrategies;
import com.continuuity.weave.zookeeper.ZKClientService;
import com.continuuity.weave.zookeeper.ZKClientServices;
import com.continuuity.weave.zookeeper.ZKClients;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Driver class to start and stop tx in distributed mode.
 */
public class OpexServiceMain {

  private static final Logger LOG = LoggerFactory.getLogger(OpexServiceMain.class);

  private static final int START = 1;
  private static final int STOP = 2;

  static void usage(boolean error) {
    PrintStream out = (error ? System.err : System.out);
    String name = OpexServiceMain.class.getSimpleName();
    Copyright.print(out);
    out.println("Usage: ");
    out.println("  " + name + " ( start | stop ) ");
  }


  public static void main(String args[]) throws Exception {

    if (args.length != 1) {
      usage(true);
      return;
    }
    if ("--help".equals(args[0])) {
      usage(false);
      return;
    }

    int command;

    if ("start".equals(args[0])) {
      command = START;
    } else if ("stop".equals(args[0])) {
      command = STOP;
    } else {
      usage(true);
      return;
    }

    DataFabricOpexModule module = new DataFabricOpexModule();
    CConfiguration conf = module.getConfiguration();

    ZKClientService zkClientService =
      ZKClientServices.delegate(
        ZKClients.reWatchOnExpire(
          ZKClients.retryOnFailure(
            ZKClientService.Builder.of(conf.get(Constants.Zookeeper.QUORUM))
              .setSessionTimeout(10000)
              .build(),
            RetryStrategies.fixDelay(2, TimeUnit.SECONDS)
          )
        )
      );
    String kafkaZKNamespace = conf.get(KafkaConstants.ConfigKeys.ZOOKEEPER_NAMESPACE_CONFIG);
    KafkaClientService kafkaClientService = new ZKKafkaClientService(
      kafkaZKNamespace == null
        ? zkClientService
        : ZKClients.namespace(zkClientService, "/" + kafkaZKNamespace)
    );

    Injector injector = Guice.createInjector(
      new MetricsClientRuntimeModule(kafkaClientService).getDistributedModules(),
      new IOModule(),
      new ConfigModule(),
      new LocationRuntimeModule().getDistributedModules(),
      new DiscoveryRuntimeModule(zkClientService).getDistributedModules(),
      module);

    // start a tx server
    final TransactionService txService = injector.getInstance(TransactionService.class);

    if (START == command) {
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          try {
            if (txService.isRunning()) {
              txService.stopAndWait();
            }
          } catch (Throwable e) {
            LOG.error("Failed to shutdown transaction service.", e);
            // because shutdown hooks execute concurrently, the logger may be closed already: thus also print it.
            System.err.println("Failed to shutdown transaction service: " + e.getMessage());
            e.printStackTrace(System.err);
          }
        }
      });

      // Starts metrics collection
      MetricsCollectionService metricsCollectionService = injector.getInstance(MetricsCollectionService.class);
      Futures.getUnchecked(Services.chainStart(zkClientService, kafkaClientService, metricsCollectionService));

      Copyright.print(System.out);
      System.out.println("Starting Operation Executor Service...");

      // Creates HBase queue table
      QueueAdmin queueAdmin = injector.getInstance(QueueAdmin.class);
      // NOTE: queues currently stored in one table, so it doesn't matter what you pass a param
      queueAdmin.create("queue");

      // start it. start is not blocking, hence we want to block to avoid termination of main
      Future<?> future = Services.getCompletionFuture(txService);
      try {
        txService.start();
      } catch (Exception e) {
        System.err.println("Failed to start service: " + e.getMessage());
      }
      // starting health/status check service
      startHealthCheckService(conf);

      future.get();
    } else {
      Copyright.print(System.out);
      System.out.println("Stopping Operation Executor Service...");
      txService.stop();
    }
  }

  private static void startHealthCheckService(CConfiguration conf) {
    int port = conf.getInt(Constants.Transaction.Service.CFG_DATA_TX_COMMAND_PORT, 0);
    final CommandPortService service = CommandPortService.builder("tx-status")
      .setPort(port)
      .addCommandHandler("ruok", "Service status", new CommandPortService.CommandHandler() {
        @Override
        public void handle(BufferedWriter respondWriter) throws IOException {
          respondWriter.write("imok");
          respondWriter.close();
        }
      })
      .build();
    service.start();
  }
}
