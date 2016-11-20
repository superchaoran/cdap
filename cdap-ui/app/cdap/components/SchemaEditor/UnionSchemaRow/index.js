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
    if (typeof props.row.type === 'object') {
      let types = props.row.type.getTypes();
      let parsedTypes = types.map(type => parseType(type));
      let displayTypes = parsedTypes.map(type => {
        return {
          type: type.displayType,
          id: uuid.v4()
        };
      });

      this.state = {
        displayTypes,
        parsedTypes
      };
    } else {
      this.state = {
        displayTypes: [
          {
            type: 'string',
            id: uuid.v4()
          }
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
    let displayTypes = this.state.displayTypes;
    displayTypes[index].type = e.target.value;
    let parsedTypes = this.state.parsedTypes;
    parsedTypes[index] = e.target.value;
    this.setState({
      displayTypes,
      parsedTypes
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
        id: uuid.v4()
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
            this.state.displayTypes.map((displayType, index) => {
              return (
                <div key={displayType.id}>
                  <SelectWithOptions
                    options={SCHEMA_TYPES.types}
                    value={displayType.type}
                    onChange={this.onTypeChange.bind(this, index)}
                  />
                  <div className="field-type"></div>
                  <div className="field-isnull">
                    <div className="btn btn-link">
                      <Input
                        type="checkbox"
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
                    checkComplexType(displayType.type) ?
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
