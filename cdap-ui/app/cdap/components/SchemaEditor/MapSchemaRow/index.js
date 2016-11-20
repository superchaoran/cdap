/*
 * Copyright © 2016 Cask Data, Inc.
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

import React, {PropTypes, Component} from 'react';
import SelectWithOptions from 'components/SelectWithOptions';
import {parseType, SCHEMA_TYPES, checkComplexType} from 'components/SchemaEditor/SchemaHelpers';
import AbstractSchemaRow from 'components/SchemaEditor/AbstractSchemaRow';
import {Input} from 'reactstrap';

require('./MapSchemaRow.less');

export default class MapSchemaRow extends Component {
  constructor(props) {
    super(props);
    if (typeof props.row.type === 'object') {
      let rowType = parseType(props.row.type);
      this.state = {
        name: rowType.name,
        keysType: rowType.type.getKeysType().getTypeName(),
        parsedKeysType: rowType.type.getKeysType().getTypeName(),
        keysTypeNullable: rowType.nullable,
        valuesTypes: rowType.type.getValuesType().getTypeName(),
        parsedValuesType: rowType.type.getValuesType().getTypeName(),
        valuesTypeNullable: rowType.nullable
      };
    } else {
      this.state = {
        name: props.row.name,
        keysType: 'string',
        parsedKeysType: 'string',
        keysTypeNullable: false,
        valuesType: 'string',
        parsedValuesType: 'string',
        valuesTypeNullable: false,
        type: props.row.type || null
      };
    }
    setTimeout(() => {
      this.props.onChange({
        type: 'map',
        keys: Array.isArray(this.state.keysTypeNullable) ? [this.state.parsedKeysType, null] : this.state.parsedKeysType,
        values: Array.isArray(this.state.valuesTypeNullable) ? [this.state.parsedValuesType, null] : this.state.parsedValuesType
      });
    });
    this.onKeysTypeChange = this.onKeysTypeChange.bind(this);
    this.onValuesTypeChange = this.onValuesTypeChange.bind(this);
  }
  onKeysTypeChange(e) {
    this.setState({
      keysType: e.target.value
    }, () => {
      this.onKeysChange(this.state.keysType);
    });
  }
  onValuesTypeChange(e) {
    this.setState({
      valuesType: e.target.value
    }, () => {
      this.onValuesChange(this.state.valuesType);
    });
  }
  onKeysChange(keysState) {
    this.setState({
      parsedKeysType: keysState
    }, () => {
      this.props.onChange({
        type: 'map',
        keys: this.state.keysTypeNullable ? [this.state.parsedKeysType, null] : this.state.parsedKeysType,
        values: this.state.valuesTypeNullable ? [this.state.parsedValuesType, null] : this.state.parsedValuesType
      });
    });
  }
  onValuesChange(valuesState) {
    this.setState({
      parsedValuesType: valuesState
    }, () => {
      this.props.onChange({
        type: 'map',
        keys: this.state.keysTypeNullable ? [this.state.parsedKeysType, null] : this.state.parsedKeysType,
        values: this.state.valuesTypeNullable ? [this.state.parsedValuesType, null] : this.state.parsedValuesType
      });
    });
  }
  onKeysTypeNullableChange(e) {
    this.setState({
      keysTypeNullable: e.target.checked
    }, () => {
      this.onKeysChange(this.state.parsedKeysType);
    });
  }
  onValuesTypeNullableChange(e) {
    this.setState({
      valuesTypeNullable: e.target.checked
    }, () => {
      this.onValuesChange(this.state.parsedValuesType);
    });
  }
  render() {
    return (
      <div className="map-schema-row">
        <div className="map-schema-kv-row">
          <div className="key-row">
            <div className="field-name">
              <span> Key </span>
              <SelectWithOptions
                options={SCHEMA_TYPES.types}
                value={this.state.keysType}
                onChange={this.onKeysTypeChange}
              />
            </div>
            <div className="field-type"></div>
            <div className="field-isnull">
              <div className="btn btn-link">
                <Input
                  type="checkbox"
                  value={this.state.keysTypeNullable}
                  onChange={this.onKeysTypeNullableChange.bind(this)}
                />
              </div>
            </div>
            {
              checkComplexType(this.state.keysType) ?
                <AbstractSchemaRow
                  row={this.state.keysType}
                  onChange={this.onKeysChange.bind(this)}
                />
              :
                null
            }
          </div>
          <div className="value-row">
            <div className="field-name">
              <span className="text-right">Value </span>
              <SelectWithOptions
                options={SCHEMA_TYPES.types}
                value={this.state.valuesType}
                onChange={this.onValuesTypeChange}
              />
            </div>
            <div className="field-type"></div>
            <div className="field-isnull">
              <div className="btn btn-link">
                <Input
                  type="checkbox"
                  value={this.state.valuesTypeNullable}
                  onChange={this.onValuesTypeNullableChange.bind(this)}
                />
              </div>
            </div>
              {
                checkComplexType(this.state.valuesType) ?
                  <AbstractSchemaRow
                    row={this.state.valuesType}
                    onChange={this.onValuesChange.bind(this)}
                  />
                :
                  null
              }
          </div>
        </div>
      </div>
    );
  }
}

MapSchemaRow.propTypes = {
  row: PropTypes.any,
  onChange: PropTypes.func.isRequired
};
