/*
 * Copyright 2015-2016 Cask Data, Inc.
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
package co.cask.cdap.data2.metadata.dataset;

import co.cask.cdap.api.dataset.DatasetAdmin;
import co.cask.cdap.api.dataset.DatasetContext;
import co.cask.cdap.api.dataset.DatasetDefinition;
import co.cask.cdap.api.dataset.DatasetProperties;
import co.cask.cdap.api.dataset.DatasetSpecification;
import co.cask.cdap.api.dataset.IncompatibleUpdateException;
import co.cask.cdap.api.dataset.Reconfigurable;
import co.cask.cdap.api.dataset.lib.AbstractDatasetDefinition;
import co.cask.cdap.api.dataset.lib.IndexedTable;

import java.io.IOException;
import java.util.Map;

/**
 * Define the Dataset for metadata.
 */
public class MetadataDatasetDefinition
  extends AbstractDatasetDefinition<MetadataDataset, DatasetAdmin>
  implements Reconfigurable {

  private static final String METADATA_INDEX_TABLE_NAME = "metadata_index";

  private final DatasetDefinition<? extends IndexedTable, ?> indexedTableDef;

  public MetadataDatasetDefinition(String name, DatasetDefinition<? extends IndexedTable, ?> indexedTableDef) {
    super(name);
    this.indexedTableDef = indexedTableDef;
  }

  // Implementation of DatasetDefinition interface methods.

  @Override
  public DatasetSpecification configure(String instanceName, DatasetProperties properties) {
    return DatasetSpecification.builder(instanceName, getName())
      .properties(properties.getProperties())
      .datasets(indexedTableDef.configure(METADATA_INDEX_TABLE_NAME,
                                          addIndexColumn(properties, MetadataDataset.INDEX_COLUMN)))
      .build();
  }

  @Override
  public DatasetSpecification reconfigure(String instanceName,
                                          DatasetProperties newProperties,
                                          DatasetSpecification currentSpec) throws IncompatibleUpdateException {
    // extract the column to index from the indexed table spec
    DatasetSpecification indexSpec = currentSpec.getSpecification(METADATA_INDEX_TABLE_NAME);
    String indexColumn = indexSpec.getProperty(IndexedTable.INDEX_COLUMNS_CONF_KEY);
    return DatasetSpecification.builder(instanceName, getName())
      .properties(newProperties.getProperties())
      .datasets(AbstractDatasetDefinition.reconfigure(indexedTableDef, METADATA_INDEX_TABLE_NAME,
                                                      addIndexColumn(newProperties, indexColumn),
                                                      indexSpec))
      .build();
  }

  private DatasetProperties addIndexColumn(DatasetProperties properties, String indexColumn) {
    return DatasetProperties
      .builder()
      .addAll(properties.getProperties())
      .add(IndexedTable.INDEX_COLUMNS_CONF_KEY, indexColumn)
      .build();
  }

  @Override
  public DatasetAdmin getAdmin(DatasetContext datasetContext, DatasetSpecification spec,
                               ClassLoader classLoader) throws IOException {
    return indexedTableDef.getAdmin(datasetContext,
                                    spec.getSpecification(METADATA_INDEX_TABLE_NAME),
                                    classLoader);
  }

  @Override
  public MetadataDataset getDataset(DatasetContext datasetContext, DatasetSpecification spec,
                                    Map<String, String> arguments, ClassLoader classLoader) throws IOException {
    return new MetadataDataset(indexedTableDef.getDataset(datasetContext,
                                                          spec.getSpecification(METADATA_INDEX_TABLE_NAME),
                                                          arguments, classLoader));
  }
}
