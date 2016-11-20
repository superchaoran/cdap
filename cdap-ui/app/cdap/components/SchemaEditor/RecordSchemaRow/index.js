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
import {SCHEMA_TYPES, checkComplexType, getParsedSchema} from 'components/SchemaEditor/SchemaHelpers';
import AbstractSchemaRow from 'components/SchemaEditor/AbstractSchemaRow';
require('./RecordSchemaRow.less');
import uuid from 'node-uuid';
import {Input} from 'reactstrap';
import SelectWithOptions from 'components/SelectWithOptions';

export default class RecordSchemaRow extends Component{
  constructor(props) {
    super(props);
    if (typeof props.row === 'object') {
      let displayFields = getParsedSchema(props.row);
      let parsedFields = displayFields.map(field => {
        let {name, type} = field;
        return {
          name,
          type
        };
      }).filter(field => field.name && field.type);
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
        parsedFields: [{
          name: '',
          type: 'string'
        }]
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
  onRowAdd(index) {
    let displayFields = this.state.displayFields;
    let parsedFields = this.state.parsedFields;
    const insertIntoArray = (arr, ele) => {
      return [
        ...arr.slice(0, index + 1),
        ele,
        ...arr.slice(index + 1, displayFields.length)
      ];
    };
    displayFields = insertIntoArray(displayFields, {
      name: '',
      displayType: 'string',
      id: uuid.v4()
    });
    parsedFields = insertIntoArray(parsedFields, {
      name: '',
      type: 'string'
    });
    this.setState({
      displayFields,
      parsedFields
    });
  }
  onRowRemove(index) {
    let displayFields = this.state.displayFields;
    let parsedFields = this.state.parsedFields;
    const removeElementAtArray = (arr, index) => {
      return [
        ...arr.slice(0, index),
        ...arr.slice(index + 1, arr.length)
      ];
    };
    displayFields = removeElementAtArray(displayFields, index);
    parsedFields = removeElementAtArray(parsedFields, index);
    this.setState({
      displayFields,
      parsedFields
    }, () => {
      let parsedFields = this.state
        .parsedFields
        .filter(field => field.name && field.type);
      this.props.onChange({
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
        name: this.state.name,
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
        name: this.state.name,
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
        name: this.state.name,
        type: 'record',
        fields: parsedFields
      });
    });
  }
  render() {
    return (
      <div className="record-schema-row">
        {
          this.state
              .displayFields
              .map((row, index) => {
                return (
                  <div
                    className="schema-row"
                    key={row.id}
                  >
                    <div className="field-name">
                      <Input
                        defaultValue={row.name}
                        onFocus={() => row.name}
                        onBlur={this.onNameChange.bind(this, index)}
                      />
                    </div>
                    <div className="field-type">
                      <SelectWithOptions
                        options={SCHEMA_TYPES.types}
                        value={row.displayType}
                        onChange={this.onTypeChange.bind(this, index)}
                      />
                    </div>
                    <div className="field-isnull">
                      <div className="btn btn-link">
                        <Input
                          type="checkbox"
                        />
                      </div>
                      <div className="btn btn-link">
                        <span
                          className="fa fa-plus fa-xs"
                          onClick={this.onRowAdd.bind(this, index)}
                        ></span>
                      </div>
                      <div className="btn btn-link">
                        {
                          this.state.displayFields.length !== 1 ?
                            <span
                              className="fa fa-trash fa-xs text-danger"
                              onClick={this.onRowRemove.bind(this, index)}
                              >
                            </span>
                          :
                            null
                        }
                      </div>
                    </div>
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
    );
  }
}

RecordSchemaRow.propTypes = {
  row: PropTypes.any,
  onChange: PropTypes.func.isRequired
};
