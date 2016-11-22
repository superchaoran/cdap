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
        parsedFields,
        error: ''
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
        }],
        error: ''
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
    let error;
    if (SCHEMA_TYPES.simpleTypes.indexOf(displayFields[index].displayType) !== -1) {
      error = this.isInvalid(parsedFields);
      if (error) {
        this.setState({ error });
        return;
      }
    }
    this.setState({
      parsedFields,
      displayFields,
      error: ''
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
  isInvalid(parsedTypes) {
    let error = '';
    let parsedType = {
      name: this.state.name,
      type: 'record',
      fields: parsedTypes
    };
    try {
      avsc.parse(parsedType);
    } catch(e) {
      error = e.message;
    }
    return error;
  }
  onTypeChange(index, e) {
    let displayFields = this.state.displayFields;
    displayFields[index].displayType = e.target.value;
    displayFields[index].type = e.target.value;
    let parsedFields = this.state.parsedFields;
    let error;
    if (displayFields[index].nullable) {
      parsedFields[index].type = [
        e.target.value,
        null
      ];
    } else {
      parsedFields[index].type = e.target.value;
    }
    if (SCHEMA_TYPES.simpleTypes.indexOf(displayFields[index].displayType) !== -1) {
      error = this.isInvalid(parsedFields);
      if (error) {
        this.setState({ error });
        return;
      }
    }
    this.setState({
      displayFields,
      parsedFields,
      error: ''
    }, () => {
      let error = this.isInvalid(this.state.parsedFields);
      if (error) {
        return;
      }
      let parsedFields = this.state
        .parsedFields
        .filter(field => field.name && field.type);
      this.props.onChange({
        name: this.state.name,
        type: 'record',
        fields: parsedFields,
      });
    });
  }
  onNullableChange(index, e) {
    let displayFields = this.state.displayFields;
    let parsedFields = this.state.parsedFields;
    displayFields[index].nullable = e.target.checked;
    if (e.target.checked) {
      parsedFields[index].type = [
        parsedFields[index].type,
        null
      ];
    } else {
      if (Array.isArray(parsedFields[index].type)) {
        parsedFields[index].type = parsedFields[index].type[0];
      }
    }
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
    let displayFields = this.state.displayFields;

    if (displayFields[index].nullable) {
      parsedFields[index].type = [
        fieldType,
        null
      ];
    } else {
      parsedFields[index].type = fieldType;
    }

    let error = this.isInvalid(parsedFields);
    if (error) {
      return;
    }
    this.setState({
      parsedFields,
      error: ''
    }, function() {
      if (this.isInvalid(this.state.parsedFields)) {
        return;
      }
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
    console.log('Inside Record Schema Render');
    return (
      <div className="record-schema-row">
        <div className="text-danger">
          {this.state.error}
        </div>
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
                          value={row.nullable}
                          onChange={this.onNullableChange.bind(this, index)}
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
