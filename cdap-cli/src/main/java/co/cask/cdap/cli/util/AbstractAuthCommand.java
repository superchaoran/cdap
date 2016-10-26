/*
 * Copyright © 2012-2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.cli.util;

import co.cask.cdap.cli.ArgumentName;
import co.cask.cdap.cli.CLIConfig;
import co.cask.cdap.cli.ElementType;
import co.cask.cdap.cli.exception.CommandInputError;
import co.cask.cdap.common.UnauthenticatedException;
import co.cask.cdap.proto.id.ApplicationId;
import co.cask.cdap.proto.id.ProgramId;
import co.cask.cdap.proto.id.ServiceId;
import co.cask.cdap.proto.security.Action;
import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Set;

/**
 * Abstract command for updating {@link co.cask.cdap.security.authentication.client.AccessToken}.
 */
public abstract class AbstractAuthCommand implements Command {

  protected static final int APP_IDX = 0;
  protected static final int VERSION_IDX = 1;
  protected static final int PROGRAM_IDX = 2;

  private static Joiner dotJoiner = Joiner.on(".");
  protected final CLIConfig cliConfig;

  protected static final Function<String, Set<Action>> ACTIONS_STRING_TO_SET = new Function<String, Set<Action>>() {
    @Override
    public Set<Action> apply(String input) {
      ImmutableSet.Builder<Action> resultBuilder = ImmutableSet.builder();
      for (String action : Splitter.on(",").trimResults().split(input)) {
        resultBuilder.add(Action.valueOf(action.toUpperCase()));
      }
      return resultBuilder.build();
    }
  };

  public AbstractAuthCommand(CLIConfig cliConfig) {
    this.cliConfig = cliConfig;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    try {
      perform(arguments, printStream);
    } catch (UnauthenticatedException e) {
      cliConfig.updateAccessToken(printStream);
      perform(arguments, printStream);
    }
  }

  public abstract void perform(Arguments arguments, PrintStream printStream) throws Exception;

  protected ServiceId parseServiceId(Arguments arguments) {
    String[] idParts = parseProgramIdArgument(arguments, ArgumentName.SERVICE.getName());
    if (idParts[VERSION_IDX] == null) {
      idParts[VERSION_IDX] =  ApplicationId.DEFAULT_VERSION;
    }
    return cliConfig.getCurrentNamespace().app(idParts[APP_IDX], idParts[VERSION_IDX]).service(idParts[PROGRAM_IDX]);
  }

  protected ServiceId parseNonversionServiceId(Arguments arguments) {
    String[] idParts = parseProgramIdArgument(arguments, ArgumentName.NON_VERSION_SERVICE.getName());
    return cliConfig.getCurrentNamespace().app(idParts[APP_IDX]).service(idParts[PROGRAM_IDX]);
  }

  protected ProgramId parseProgramId(Arguments arguments, ElementType elementType) {
    String[] idParts = parseProgramIdArgument(arguments, elementType.getArgumentName().getName());
    if (idParts[VERSION_IDX] == null) {
      idParts[VERSION_IDX] =  ApplicationId.DEFAULT_VERSION;
    }
    return cliConfig.getCurrentNamespace().app(idParts[APP_IDX], idParts[VERSION_IDX])
      .program(elementType.getProgramType(), idParts[PROGRAM_IDX]);
  }

  protected String[] parseProgramIdArgument(Arguments arguments, String argumentName) {
    String[] argumentParts = arguments.get(argumentName).split("\\.");
    if (argumentParts.length < 2) {
      throw new CommandInputError(this);
    }

    String appName = argumentParts[0];
    String programName = argumentParts[argumentParts.length - 1];
    String appVersion = argumentParts.length == 2 ? null :
      dotJoiner.join(Arrays.copyOfRange(argumentParts, 1, argumentParts.length - 1));
    return new String[] {appName, appVersion, programName};
  }

  protected ApplicationId parseApplicationId(Arguments arguments) {
    String appVersion = arguments.hasArgument(ArgumentName.APP_VERSION.toString()) ?
      arguments.get(ArgumentName.APP_VERSION.toString()) : ApplicationId.DEFAULT_VERSION;
    return cliConfig.getCurrentNamespace().app(arguments.get(ArgumentName.APP.toString()), appVersion);
  }
}
