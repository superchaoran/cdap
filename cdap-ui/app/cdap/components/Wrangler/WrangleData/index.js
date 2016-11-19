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
import WranglerStore from 'components/Wrangler/Redux/WranglerStore';

import RenameAction from 'components/Wrangler/ColumnActions/RenameAction';
import ColumnActionsDropdown from 'components/Wrangler/ColumnActionsDropdown';

export default class WrangleData extends Component {
  constructor(props) {
    super(props);

    let wrangler = WranglerStore.getState().wrangler;

    let stateObj = Object.assign({}, wrangler, {
      loading: false,
      activeSelection: null,
      activeSelectionType: null,
      isSplit: false,
      isMerge: false,
      isSubstring: false
    });

    this.state = stateObj;

    WranglerStore.subscribe(() => {
      let state = WranglerStore.getState().wrangler;
      this.setState(state);
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
                        <th key={head}>
                          <RenameAction column={head} />
                          {head} ({this.state.columnTypes[head]})
                          {errors[head] && errors[head].count ? errorCircle : null}

                          <ColumnActionsDropdown column={head} />
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
