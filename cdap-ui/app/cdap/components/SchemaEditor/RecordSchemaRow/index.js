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
import {SCHEMA_TYPES, checkComplexType} from 'components/SchemaEditor/SchemaHelpers';
import AbstractSchemaRow from 'components/SchemaEditor/AbstractSchemaRow';
require('./RecordSchemaRow.less');
import uuid from 'node-uuid';
import {Input} from 'reactstrap';
import SelectWithOptions from 'components/SelectWithOptions';

export default class RecordSchemaRow extends Component{
  constructor(props) {
    super(props);
    if (typeof props.row === 'object') {
      let displayFields = props.row;
      let parsedFields = displayFields.map(field => {
        let {name, type} = field;
        return {
          name,
          type
        };
      });
      this.state = {
        type: 'record',
        name: 'a' +  uuid.v4().split('-').join(''),
        displayFields,
        parsedFields
      };
    } else {
      this.state = {
        type: 'record',
        name: 'a' +  uuid.v4().split('-').join(''),
        displayFields: [
          {
            name: '',
            type: 'string',
            displayType: 'string',
            nullable: false,
            id: 'a' + uuid.v4().split('-').join(''),
            nested: false
          }
        ],
        parsedFields: [
          {
            name: '',
            type: 'string'
          }
        ]
      };
    }
    setTimeout(() => {
      let parsedFields = this.state
        .parsedFields
        .filter(field => field.name && field.type);
      props.onChange({
        type: 'record',
        name: this.state.name,
        fields: parsedFields
      });
    });
  }
  onNameChange(index, e) {
    let displayFields = this.state.displayFields;
    let parsedFields = this.state.parsedFields;
    displayFields[index].name = e.target.value;
    parsedFields[index].name = e.target.value;
    this.setState({
      parsedFields,
      displayFields
    }, () => {
      let parsedFields = this.state
        .parsedFields
        .filter(field => field.name && field.type);
      this.props.onChange({
        name: 'a' + uuid.v4().split('-').join(''),
        type: 'record',
        fields: parsedFields
      });
    });
  }
  onTypeChange(index, e) {
    let displayFields = this.state.displayFields;
    displayFields[index].displayType = e.target.value;
    displayFields[index].type = e.target.value;
    let parsedFields = this.state.parsedFields;
    parsedFields[index].type = e.target.value;
    this.setState({
      displayFields,
      parsedFields
    }, () => {
      let parsedFields = this.state
        .parsedFields
        .filter(field => field.name && field.type);
      this.props.onChange({
        name: 'a' + uuid.v4().split('-').join(''),
        type: 'record',
        fields: parsedFields
      });
    });
  }
  onChange(index, fieldType) {
    let parsedFields = this.state.parsedFields;
    parsedFields[index].type = fieldType;
    this.setState({
      parsedFields
    }, () => {
      let parsedFields = this.state
        .parsedFields
        .filter(field => field.name && field.type);
      this.props.onChange({
        name: 'a' + uuid.v4().split('-').join(''),
        type: 'record',
        fields: parsedFields
      });
    });
  }
  render() {
    return (
      <div className="record-schema-row">
        <div className="record-schema-records-row">
          {
            this.state
                .displayFields
                .map((row, index) => {
                  return (
                    <div
                      className="schema-row"
                      key={index}
                    >
                      <div className="field-name">
                        <Input
                          value={row.name}
                          onChange={this.onNameChange.bind(this, index)}
                        />
                      </div>
                      <div className="field-type">
                        <SelectWithOptions
                          options={SCHEMA_TYPES.types}
                          value={row.displayType}
                          onChange={this.onTypeChange.bind(this, index)}
                        />
                      </div>
                      <div className="field-isnull text-center">TBD</div>
                      {
                        checkComplexType(row.displayType) ?
                          <AbstractSchemaRow
                            row={row.type}
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

RecordSchemaRow.propTypes = {
  row: PropTypes.any,
  onChange: PropTypes.func.isRequired
};
