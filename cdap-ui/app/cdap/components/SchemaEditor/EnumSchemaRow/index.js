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
import {parseType} from 'components/SchemaEditor/SchemaHelpers';
import {Input} from 'reactstrap';
import {insertAt, removeAt} from 'services/helpers';
import uuid from 'node-uuid';
require('./EnumSchemaRow.less');

export default class EnumSchemaRow extends Component {
  constructor(props) {
    super(props);
    if (typeof props.row === 'object') {
      let rowType = parseType(props.row);
      let symbols = rowType.type.getSymbols();
      this.state = {
        symbols,
        error: ''
      };
    } else {
      this.state = {
        symbols: [''],
        error: ''
      };
    }
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
  onSymbolChange(index, e) {
    let symbols = this.state.symbols;
    symbols[index] = e.target.value;
    let error = this.isInvalid({
      type: 'enum',
      symbols
    });
    if (error) {
      this.setState({error});
      return;
    }
    this.setState({
      symbols,
      error: ''
    }, () => {
      this.props.onChange({
        type: 'enum',
        symbols: this.state.symbols
      });
    });
  }

  onSymbolAdd(index, e) {
    let symbols = this.state.symbols;
    symbols = insertAt(symbols, index, e.target.value || '');
    this.setState({symbols}, () => {
      let error = this.isInvalid({
        type: 'enum',
        symbols
      });
      if (error) {
        return;
      }
      this.props.onChange({
        type: 'enum',
        symbols: this.state.symbols
      });
    });
  }

  onSymbolRemove(index) {
    let symbols = this.state.symbols;
    symbols = removeAt(symbols, index);
    this.setState({
      symbols
    }, () => {
      this.props.onChange({
        type: 'enum',
        symbols: this.state.symbols
      });
    });
  }

  render() {
    return (
      <div className="enum-schema-row">
        <div className="text-danger">
          {this.state.error}
        </div>
        {
          this.state.symbols.map((symbol, index) => {
            return (
              <div
                className="enum-schema-symbols-row"
                key={uuid.v4()}
              >
                <Input
                  className="field-name"
                  defaultValue={symbol}
                  onFocus={() => symbol}
                  onBlur={this.onSymbolChange.bind(this, index)}
                />
                <div className="field-type"></div>
                <div className="field-isnull">
                  <div className="btn btn-link"></div>
                  <div className="btn btn-link">
                    <span
                      className="fa fa-plus"
                      onClick={this.onSymbolAdd.bind(this, index)}
                    ></span>
                  </div>
                  <div className="btn btn-link">
                    {
                      this.state.symbols.length !== 1 ?
                        <span
                          className="fa fa-trash text-danger"
                          onClick={this.onSymbolRemove.bind(this, index)}
                        ></span>
                      :
                        null
                    }
                  </div>
                </div>
              </div>
            );
          })
        }
      </div>
    );
  }
}

EnumSchemaRow.propTypes = {
  row: PropTypes.any,
  onChange: PropTypes.func.isRequired
};
