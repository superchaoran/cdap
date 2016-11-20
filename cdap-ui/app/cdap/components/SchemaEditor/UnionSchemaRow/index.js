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
import {parseType, SCHEMA_TYPES, checkComplexType} from 'components/SchemaEditor/SchemaHelpers';
import SelectWithOptions from 'components/SelectWithOptions';
import AbstractSchemaRow from 'components/SchemaEditor/AbstractSchemaRow';

require('./UnionSchemaRow.less');

export default class UnionSchemaRow extends Component {
  constructor(props) {
    super(props);
    if (typeof props.row.type === 'object') {
      let types = props.row.type.getTypes();
      let parsedTypes = types.map(type => parseType(type));
      let displayTypes = parsedTypes.map(type => type.displayType);

      this.state = {
        types: displayTypes,
        parsedTypes
      };
    } else {
      this.state = {
        types: [
          'string'
        ],
        parsedTypes: [
          'string'
        ]
      };
    }
    setTimeout(() => {
      props.onChange(this.state.parsedTypes);
    });
    this.onTypeChange = this.onTypeChange.bind(this);
  }
  onTypeChange(index, e) {
    let types = this.state.types;
    types[index] = e.target.value;
    let parsedTypes = this.state.parsedTypes;
    parsedTypes[index] = e.target.value;
    this.setState({
      types,
      parsedTypes
    }, () => {
      this.props.onChange(this.state.parsedTypes);
    });
  }
  onTypeAdd(index) {
    let types = this.state.types;
    let parsedTypes = this.state.parsedTypes;
    types = [
      ...types.slice(0, index + 1),
      'string',
      ...types.slice(index + 1, types.length)
    ];
    parsedTypes = [
      ...parsedTypes.slice(0, index + 1),
      'string',
      ...parsedTypes.slice(index + 1, parsedTypes.length)
    ];
    this.setState({ types, parsedTypes });
    this.props.onChange(this.state.parsedTypes);
  }
  onChange(index, parsedType) {
    let parsedTypes = this.state.parsedTypes;
    parsedTypes[index] = parsedType;
    this.setState({parsedTypes}, () => {
      this.props.onChange(this.state.parsedTypes);
    });
  }
  render() {
    return (
      <div className="union-schema-row">
        <div className="union-schema-types-row">
          {
            this.state.types.map((type, index) => {
              return (
                <div key={index}>
                  <SelectWithOptions
                    options={SCHEMA_TYPES.types}
                    value={type}
                    onChange={this.onTypeChange.bind(this, index)}
                  />
                  <div className="field-type"></div>
                  <div className="field-isnull">
                    <div className="btn btn-link">
                      <span
                        className="fa fa-plus"
                        onClick={this.onTypeAdd.bind(this, index)}
                      ></span>
                    </div>
                    <div className="btn btn-link">
                      <span
                        className="fa fa-trash fa-xs text-danger"
                        >
                      </span>
                    </div>
                  </div>
                  {
                    checkComplexType(type) ?
                      <AbstractSchemaRow
                        row={type}
                        onChange={this.onChange.bind(this, index)}
                      />
                    :
                      null
                  }
                </div>
              );
            })
          }
        </div>
      </div>
    );
  }
}
UnionSchemaRow.propTypes = {
  row: PropTypes.any,
  onChange: PropTypes.func.isRequired
};
