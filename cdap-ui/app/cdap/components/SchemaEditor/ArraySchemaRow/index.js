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

require('./ArraySchemaRow.less');

export default class ArraySchemaRow extends Component{
  constructor(props) {
    super(props);
    if (typeof props.row === 'object') {
      let item = parseType(props.row.getItemsType());
      this.state = {
        displayType: {
          type: item.displayType,
          nullable: item.nullable
        },
        parsedType: props.row.getItemsType(),
        error: ''
      };
    } else {
      this.state = {
        displayType: {
          type: props.row.type || 'string',
          nullable: false
        },
        parsedType: props.row.type || 'string',
        error: ''
      };
    }
    this.onTypeChange = this.onTypeChange.bind(this);
    setTimeout(() => {
      props.onChange({
        type: 'array',
        items: this.state.parsedType
      });
    });
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
  onTypeChange(e) {
    if (SCHEMA_TYPES.simpleTypes.indexOf(e.target.value) !== -1) {
      let error = this.isInvalid({
        type: 'array',
        items: e.target.value
      });
      if (error) {
        this.setState({
          error
        });
        return;
      }
    }
    this.setState({
      displayType: {
        type: e.target.value,
        nullable: this.state.displayType.nullable
      },
      parsedType: e.target.value,
      error: ''
    }, () => {
      let error = this.isInvalid({
        type: 'array',
        items: this.state.parsedType
      });
      if (error) {
        return;
      }
      this.props.onChange({
        type: 'array',
        items: this.state.parsedType
      });
    });
  }
  onNullableChange(e) {
    let error = this.isInvalid({
      type: 'array',
      items: e.target.value
    });
    if (error) {
      this.setState({
        error
      });
      return;
    }
    this.setState({
      displayType: {
        type: this.state.displayType.type,
        nullable: e.target.checked
      },
      error: ''
    }, () => {
      if (this.state.displayType.nullable) {
        this.props.onChange({
          type: 'array',
          items: [
            this.state.parsedType,
            null
          ]
        });
      } else {
        this.props.onChange({
          type: 'array',
          items: this.state.parsedType
        });
      }
    });
  }
  onChange(itemsState) {
    let error = this.isInvalid({
      type: 'array',
      items: this.state.displayType.nullable ? [itemsState, 'null'] : itemsState
    });
    if (error) {
      return;
    }
    this.setState({
      parsedType: itemsState,
      error: ''
    });
    if (this.state.displayType.nullable) {
      this.props.onChange({
        type: 'array',
        items: [
          itemsState,
          'null'
        ]
      });
    } else {
      this.props.onChange({
        type: 'array',
        items: itemsState
      });
    }
  }
  render() {
    return (
      <div className="array-schema-row">
        <div className="text-danger">
          {this.state.error}
        </div>
        <div className="array-schema-type-row">
          <div className="field-name">
            <SelectWithOptions
              options={SCHEMA_TYPES.types}
              value={this.state.displayType.type}
              onChange={this.onTypeChange}
            />
          </div>
          <div className="field-type"></div>
          <div className="field-isnull">
            <div className="btn btn-link">
              <Input
                type="checkbox"
                value={this.state.displayType.nullable}
                onChange={this.onNullableChange.bind(this)}
              />
            </div>
          </div>
        </div>
        {
          checkComplexType(this.state.displayType.type) ?
            <AbstractSchemaRow
              row={this.state.parsedType}
              onChange={this.onChange.bind(this)}
            />
          :
            null
        }
      </div>
    );
  }
}

ArraySchemaRow.propTypes = {
  row: PropTypes.any,
  onChange: PropTypes.func.isRequired
};
