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
    if (typeof props.row.type === 'object') {
      let item = parseType(props.row.type.getItemsType());
      this.state = {
        displayType: {
          type: item.displayType,
          nullable: item.nullable
        },
        parsedType: props.row.type.getItemsType()
      };
    } else {
      this.state = {
        displayType: {
          type: props.row.type || 'string',
          nullable: false
        },
        parsedType: props.row.type || 'string'
      };
    }
    this.onTypeChange = this.onTypeChange.bind(this);
    setTimeout(() => {
      props.onChange({
        type: 'array',
        items: this.state.displayType.type
      });
    });
  }
  onTypeChange(e) {
    this.setState({
      displayType: {
        type: e.target.value,
        nullable: this.state.displayType.nullable
      }
    }, () => {
      this.props.onChange({
        type: 'array',
        items: this.state.displayType.type
      });
    });
  }
  onNullableChange(e) {
    this.setState({
      displayType: {
        type: this.state.displayType.type,
        nullable: e.target.checked
      }
    }, () => {
      if (this.state.displayType.nullable) {
        this.props.onChange({
          type: 'array',
          items: [
            this.state.displayType.type,
            null
          ]
        });
      } else {
        this.props.onChange({
          type: 'array',
          items: this.state.displayType.type
        });
      }
    });
  }
  onChange(itemsState) {
    if (this.state.displayType.nullable) {
      this.props.onChange({
        type: 'array',
        items: [
          itemsState,
          null
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
              row={this.state.displayType.type}
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
