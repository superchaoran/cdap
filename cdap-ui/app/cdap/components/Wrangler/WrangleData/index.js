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

import React, { Component, PropTypes } from 'react';
import WrangleHistory from 'components/Wrangler/WrangleHistory';
import classnames from 'classnames';
import shortid from 'shortid';
import Histogram from 'components/Wrangler/Histogram';
import WranglerActions from 'components/Wrangler/Redux/WranglerActions';
import WranglerStore from 'components/Wrangler/Redux/WranglerStore';

export default class WrangleData extends Component {
  constructor(props) {
    super(props);

    let wrangler = WranglerStore.getState().wrangler;

    let stateObj = Object.assign({}, wrangler, {
      loading: false,
      activeSelection: null,
      activeSelectionType: null,
      isRename: false,
      isSplit: false,
      isMerge: false,
      isSubstring: false
    });

    this.state = stateObj;

    this.dropColumn = this.dropColumn.bind(this);
    this.splitColumnClick = this.splitColumnClick.bind(this);
    this.mergeColumnClick = this.mergeColumnClick.bind(this);
    this.renameColumnClick = this.renameColumnClick.bind(this);
    this.onRename = this.onRename.bind(this);
    this.onSplit = this.onSplit.bind(this);
    this.onMerge = this.onMerge.bind(this);
    this.substringColumnClick = this.substringColumnClick.bind(this);
    this.onSubstring = this.onSubstring.bind(this);


    WranglerStore.subscribe(() => {
      let state = WranglerStore.getState().wrangler;
      this.setState(state);
    });
  }

  renderActionList() {
    if (this.state.activeSelectionType === 'COLUMN') {
      return this.renderColumnActions();
    } else {
      return null;
    }
  }

  renderColumnActions() {
    return (
      <div className="btn-group-vertical">
        <button
          className="btn btn-default"
          onClick={this.dropColumn}
        >
          Drop column
        </button>
        <button
          className="btn btn-default"
          onClick={this.splitColumnClick}
        >
          Split column
        </button>
        {this.renderSplit()}

        <button
          className="btn btn-default"
          onClick={this.mergeColumnClick}
        >
          Merge column
        </button>
        {this.renderMerge()}

        <button
          className="btn btn-default"
          onClick={this.renameColumnClick}
        >
          Rename column
        </button>
        {this.renderRename()}

        <button
          className="btn btn-default"
          onClick={this.transformCase.bind(this, 'UPPERCASE')}
        >
          UPPER CASE
        </button>

        <button
          className="btn btn-default"
          onClick={this.transformCase.bind(this, 'LOWERCASE')}
        >
          lower case
        </button>

        <button
          className="btn btn-default"
          onClick={this.transformCase.bind(this, 'TITLECASE')}
        >
          Title Case
        </button>

        <button
          className="btn btn-default"
          onClick={this.substringColumnClick}
        >
          Substring
        </button>
        {this.renderSubstring()}
      </div>
    );
  }

  columnClickHandler(column) {
    this.setState({
      activeSelectionType: 'COLUMN',
      activeSelection: column
    });
  }

  renderRename() {
    if (!this.state.isRename) { return null; }

    return (
      <div className="rename-input">
        <label className="label-control">New name</label>
        <input
          type="text"
          className="form-control"
          ref={(ref) => this.renameInput = ref}
        />
        <button
          className="btn btn-success"
          onClick={this.onRename}
        >
          Save
        </button>
      </div>
    );
  }

  renderSplit() {
    if (!this.state.isSplit) { return null; }

    return (
      <div className="split-input">
        <div>
          <label className="control-label">Split by first occurence of:</label>
          <input
            type="text"
            className="form-control"
            ref={(ref) => this.splitDelimiter = ref}
          />
        </div>
        <div>
          <label className="control-label">First Split Name:</label>
          <input
            type="text"
            className="form-control"
            ref={(ref) => this.firstSplit = ref}
          />
        </div>
        <div>
          <label className="control-label">Second Split Name:</label>
          <input
            type="text"
            className="form-control"
            ref={(ref) => this.secondSplit = ref}
          />
        </div>
        <button
          className="btn btn-success"
          onClick={this.onSplit}
        >
          Save
        </button>
      </div>
    );
  }

  renderMerge() {
    if (!this.state.isMerge) { return null; }

    let headers = Object.keys(this.state.data[0]);

    // remove currently selected from list of columns
    headers.splice(headers.indexOf(this.state.activeSelection), 1);

    return (
      <div className="merge-input">
        <div>
          <label className="control-label">Merge with</label>
          <select
            defaultValue={headers[0]}
            ref={(ref) => this.mergeWith = ref}
          >
            {
              headers.map((header) => {
                return (
                  <option
                    value={header}
                    key={header}
                  >
                    {header}
                  </option>
                );
              })
            }
          </select>
        </div>
        <div>
          <label className="control-label">Join by:</label>
          <input
            type="text"
            className="form-control"
            ref={(ref) => this.joinBy = ref}
          />
        </div>
        <div>
          <label className="control-label">Merged Column Name:</label>
          <input
            type="text"
            className="form-control"
            ref={(ref) => this.mergedColumnName = ref}
          />
        </div>
        <button
          className="btn btn-success"
          onClick={this.onMerge}
        >
          Save
        </button>
      </div>
    );
  }

  renderSubstring() {
    if (!this.state.isSubstring) { return null; }

    return (
      <div className="substring-input">
        <div>
          <label className="control-label">Begin Index:</label>
          <input
            type="number"
            className="form-control"
            onChange={e => this.setState({substringBeginIndex: e.target.value})}
          />
        </div>

        <div>
          <label className="control-label">End Index:</label>
          <input
            type="number"
            className="form-control"
            onChange={e => this.setState({substringEndIndex: e.target.value})}
          />
        </div>

        <div>
          <label className="control-label">New Column Name:</label>
          <input
            type="text"
            className="form-control"
            onChange={e => this.setState({substringColumnName: e.target.value})}
          />
        </div>
        <button
          className="btn btn-success"
          onClick={this.onSubstring}
        >
          Save
        </button>
      </div>
    );
  }


  // DROP COLUMN
  dropColumn() {
    const columnToDrop = this.state.activeSelection;

    WranglerStore.dispatch({
      type: WranglerActions.dropColumn,
      payload: {
        activeColumn: columnToDrop
      }
    });
  }


  // RENAME
  renameColumnClick() {
    this.setState({
      isMerge: false,
      isSplit: false,
      isRename: true,
      isSubstring: false
    });
  }

  onRename() {
    const originalName = this.state.activeSelection;
    const newName = this.renameInput.value;

    WranglerStore.dispatch({
      type: WranglerActions.renameColumn,
      payload: {
        activeColumn: originalName,
        newName: newName
      }
    });
  }

  // SPLIT
  splitColumnClick() {
    this.setState({
      isMerge: false,
      isSplit: true,
      isRename: false,
      isSubstring: false
    });
  }

  onSplit() {
    const delimiter = this.splitDelimiter.value;
    const firstSplit = this.firstSplit.value;
    const secondSplit = this.secondSplit.value;
    const columnToSplit = this.state.activeSelection;

    WranglerStore.dispatch({
      type: WranglerActions.splitColumn,
      payload: {
        activeColumn: columnToSplit,
        firstSplit,
        secondSplit,
        delimiter
      }
    });
  }

  // MERGE
  mergeColumnClick() {
    this.setState({
      isMerge: true,
      isSplit: false,
      isRename: false,
      isSubstring: false
    });
  }

  onMerge() {
    const mergeWith = this.mergeWith.value;
    const columnToMerge = this.state.activeSelection;
    const joinBy = this.joinBy.value;
    const mergedColumnName = this.mergedColumnName.value;

    WranglerStore.dispatch({
      type: WranglerActions.mergeColumn,
      payload: {
        activeColumn: columnToMerge,
        joinBy,
        mergedColumnName,
        mergeWith
      }
    });
  }

  // CASE TRANSFORMATIONS
  transformCase(type) {
    const columnToTransform = this.state.activeSelection;

    let actionType;
    switch (type) {
      case 'UPPERCASE':
        actionType = WranglerActions.upperCaseColumn;
        break;
      case 'LOWERCASE':
        actionType = WranglerActions.lowerCaseColumn;
        break;
      case 'TITLECASE':
        actionType = WranglerActions.titleCaseColumn;
        break;
    }

    WranglerStore.dispatch({
      type: actionType,
      payload: {
        activeColumn: columnToTransform
      }
    });
  }

  // SUBSTRING
  substringColumnClick() {
    this.setState({
      isRename: false,
      isSplit: false,
      isMerge: false,
      isSubstring: true
    });
  }

  onSubstring() {
    const columnToSub = this.state.activeSelection;
    const beginIndex = this.state.substringBeginIndex;
    const endIndex = this.state.substringEndIndex;
    const columnName = this.state.substringColumnName;

    WranglerStore.dispatch({
      type: WranglerActions.subStringColumn,
      payload: {
        activeColumn: columnToSub,
        beginIndex,
        endIndex,
        columnName
      }
    });
  }

  render() {
    if (this.state.loading) {
      return (
        <div className="loading text-center">
          <div>
            <span className="fa fa-spinner fa-spin"></span>
          </div>
          <h3>Wrangling...</h3>
        </div>
      );
    }


    const headers = this.state.headersList;
    const data = this.state.data;
    const errors = this.state.errors;

    const errorCount = headers.reduce((prev, curr) => {
      let count = errors[curr] ? errors[curr].count : 0;
      return prev + count;
    }, 0);

    const errorCircle = <i className="fa fa-circle error pull-right"></i>;

    return (
      <div className="wrangler-data row">
        <div className="wrangle-transforms">
          <div className="wrangle-filters text-center">
            <span className="fa fa-undo"></span>
            <span className="fa fa-repeat"></span>
            <span className="fa fa-filter"></span>
          </div>

          <h4>Actions</h4>

          {this.renderActionList()}

          <hr/>
          <h4>History</h4>

          <WrangleHistory
            historyArray={this.state.history}
          />

        </div>

        <div className="wrangle-results">
          <div className="wrangler-data-metrics">
            <div className="metric-block">
              <h3>{this.state.data.length}</h3>
              <h5>Rows</h5>
            </div>

            <div className="metric-block">
              <h3>{this.state.headersList.length}</h3>
              <h5>Columns</h5>
            </div>

            <div className="metric-block">
              <h3 className="text-danger">{errorCount}</h3>
              <h5>Errors</h5>
            </div>
          </div>

          <div className="data-table">
            <table className="table table-bordered">
              <thead>
                <tr>
                  <th className="index-column">#</th>
                  {
                    headers.map((head) => {
                      return (
                        <th
                          key={head}
                          onClick={this.columnClickHandler.bind(this, head)}
                          className={classnames('column-name', {
                            active: this.state.activeSelection === head
                          })}
                        >
                          {head} ({this.state.columnTypes[head]})
                          {errors[head] && errors[head].count ? errorCircle : null}
                        </th>
                      );
                    })
                  }
                </tr>
                <tr>
                  <th></th>
                  {
                    headers.map((head) => {
                      return (
                        <th key={head}>
                          <Histogram
                            data={this.state.histogram[head].data}
                            labels={this.state.histogram[head].labels}
                          />
                        </th>
                      );
                    })
                  }
                </tr>
              </thead>

              <tbody>
                { data.map((row, index) => {
                  return (
                    <tr key={shortid.generate()}>
                      <td className="index-column">
                        <span className="content">{index+1}</span>
                      </td>
                      {
                        headers.map((head) => {
                          return (
                            <td
                              key={shortid.generate()}
                              className={classnames({
                                active: this.state.activeSelection === head
                              })}
                            >
                              <span className="content">{row[head]}</span>
                              {errors[head] && errors[head][index] ? errorCircle : null}
                            </td>
                          );
                        })
                      }
                    </tr>
                  );
                }) }
              </tbody>
            </table>
          </div>
        </div>
      </div>
    );
  }
}

WrangleData.defaultProps = {
  data: []
};

WrangleData.propTypes = {
  data: PropTypes.arrayOf(PropTypes.object)
};
