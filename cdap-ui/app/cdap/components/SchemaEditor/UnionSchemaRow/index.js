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
import {Input} from 'reactstrap';
import uuid from 'node-uuid';

require('./UnionSchemaRow.less');

export default class UnionSchemaRow extends Component {
  constructor(props) {
    super(props);
    if (typeof props.row === 'object') {
      let parsedTypes = props.row.getTypes();
      let displayTypes = parsedTypes.map(type => Object.assign({}, parseType(type), {id: 'a' + uuid.v4()}));

      this.state = {
        displayTypes,
        parsedTypes,
        error: ''
      };
    } else {
      this.state = {
        displayTypes: [
          {
            displayType: 'string',
            type: 'string',
            id: uuid.v4(),
            nullable: false
          }
        ],
        parsedTypes: [
          'string'
        ],
        error: ''
      };
    }
    setTimeout(() => {
      props.onChange(this.state.parsedTypes);
    });
    this.onTypeChange = this.onTypeChange.bind(this);
  }
  isInvalid(parsedTypes) {
    let error = '';
    try {
      avsc.parse(parsedTypes);
    } catch(e) {
      error = e.message;
    }
    return error;
  }
  onNullableChange(index, e) {
    let displayTypes = this.state.displayTypes;
    let parsedTypes = this.state.parsedTypes;
    displayTypes[index].nullable = e.target.checked;
    let error = '';
    if (e.target.checked) {
      parsedTypes[index] = [
        parsedTypes[index],
        null
      ];
    } else {
      parsedTypes[index] = parsedTypes[index][0];
    }
    this.setState({
      displayTypes,
      parsedTypes,
      error
    }, () => {
      this.props.onChange(this.state.parsedTypes);
    });
  }
  onTypeChange(index, e) {
    let displayTypes = this.state.displayTypes;
    displayTypes[index].displayType = e.target.value;
    displayTypes[index].type = e.target.value;
    let parsedTypes = this.state.parsedTypes;
    let error = '';
    if (displayTypes[index].nullable) {
      parsedTypes[index] = [
        e.target.value,
        null
      ];
    } else {
      parsedTypes[index] = e.target.value;
    }
    if (SCHEMA_TYPES.simpleTypes.indexOf(e.target.value) !== -1) {
      error = this.isInvalid(parsedTypes);
      if (error) {
        this.setState({ error });
        return;
      }
    }
    this.setState({
      displayTypes,
      parsedTypes,
      error
    }, () => {
      this.props.onChange(this.state.parsedTypes);
    });
  }
  onTypeAdd(index) {
    let displayTypes = this.state.displayTypes;
    let parsedTypes = this.state.parsedTypes;
    displayTypes = [
      ...displayTypes.slice(0, index + 1),
      {
        type: 'string',
        id: uuid.v4(),
        nullable: false
      },
      ...displayTypes.slice(index + 1, displayTypes.length)
    ];
    parsedTypes = [
      ...parsedTypes.slice(0, index + 1),
      'string',
      ...parsedTypes.slice(index + 1, parsedTypes.length)
    ];
    this.setState({ displayTypes, parsedTypes }, () => {
      this.props.onChange(this.state.parsedTypes);
    });
  }
  onTypeRemove(index) {
    let displayTypes = this.state.displayTypes;
    let parsedTypes = this.state.parsedTypes;
    displayTypes = [
      ...displayTypes.slice(0, index),
      ...displayTypes.slice(index + 1, displayTypes.length)
    ];
    parsedTypes = [
      ...parsedTypes.slice(0, index),
      ...parsedTypes.slice(index + 1, parsedTypes.length)
    ];
    this.setState({ displayTypes, parsedTypes }, () => {
      this.props.onChange(this.state.parsedTypes);
    });
  }
  onChange(index, parsedType) {
    let parsedTypes = this.state.parsedTypes;
    let displayTypes = this.state.displayTypes;
    let error;
    if (displayTypes[index].nullable) {
      parsedTypes[index] = [
        parsedType,
        null
      ];
    } else {
      parsedTypes[index] = parsedType;
    }
    error = this.isInvalid(parsedTypes);
    this.setState({parsedTypes, error}, () => {
      this.props.onChange(this.state.parsedTypes);
    });
  }
  render() {
    return (
      <div className="union-schema-row">
        <div className="text-danger">
          {this.state.error}
        </div>
        <div className="union-schema-types-row">
          {
            this.state.displayTypes.map((displayType, index) => {
              return (
                <div key={displayType.id}>
                  <SelectWithOptions
                    options={SCHEMA_TYPES.types}
                    value={displayType.displayType}
                    onChange={this.onTypeChange.bind(this, index)}
                  />
                  <div className="field-type"></div>
                  <div className="field-isnull">
                    <div className="btn btn-link">
                      <Input
                        type="checkbox"
                        value={displayType.nullable}
                        onChange={this.onNullableChange.bind(this, index)}
                      />
                    </div>
                    <div className="btn btn-link">
                      <span
                        className="fa fa-plus"
                        onClick={this.onTypeAdd.bind(this, index)}
                      ></span>
                    </div>
                    <div className="btn btn-link">
                      {
                        this.state.displayTypes.length !== 1 ?
                          <span
                            className="fa fa-trash fa-xs text-danger"
                            onClick={this.onTypeRemove.bind(this, index)}
                          >
                          </span>
                        :
                          null
                      }
                    </div>
                  </div>
                  {
                    checkComplexType(displayType.displayType) ?
                      <AbstractSchemaRow
                        row={displayType.type}
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
