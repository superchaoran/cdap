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

import React, { Component } from 'react';
import Papa from 'papaparse';
import WrangleData from 'components/Wrangler/WrangleData';
require('./Wrangler.less');

/**
 * 3 Steps for any transforms:
 *    1. Format the data
 *    2. Handle the ordering of Headers
 *    3. Handle the inferred type of the column
 **/
export default class Wrangler extends Component {
  constructor(props) {
    super(props);

    this.state = {
      header: false,
      skipEmptyLines: false,
      delimiter: '',
      wranglerInput: '',
      originalData: []
    };

    this.handleSetHeaders = this.handleSetHeaders.bind(this);
    this.handleSetSkipEmptyLines = this.handleSetSkipEmptyLines.bind(this);
    this.setDelimiter = this.setDelimiter.bind(this);
    this.wrangle = this.wrangle.bind(this);
    this.handleData = this.handleData.bind(this);
    this.handleTextInput = this.handleTextInput.bind(this);
  }

  componentDidMount() {
    this.wrangle();
  }

  wrangle() {
    // let input = this.state.wranglerInput;

    let input = `Mildred,Ramirez,6 Blackbird Drive,Lexington,KY,40515
Ralph,Hawkins,8 1st Plaza,Washington,DC,20580
Albert,Barnes,959 Continental Place,Washington,DC,20436
Ruby,Burns,7821 Straubel Center,Fort Worth,TX,76110
Todd,Day,34 Annamark Road,Louisville,KY,40210
Robert,Alvarez,3 Burrows Center,New York City,NY,10170
David,Morales,638 Sachtjen Trail,New Haven,CT,06533
Charles,Carter,050 Grayhawk Drive,Gaithersburg,MD,20883
Martin,Cruz,66942 Texas Pass,Wilmington,DE,19810
Phillip,Oliver,64 Bonner Junction,Pasadena,TX,77505
Margaret,Turner,754 Carey Parkway,Erie,PA,16550
Jeremy,Oliver,07412 Charing Cross Junction,Austin,TX,78744
Jeffrey,Johnston,3960 Hoffman Terrace,Charleston,WV,25331
Michael,Hawkins,9 Clyde Gallagher Alley,West Hartford,CT,06127
Phyllis,Owens,134 Farwell Park,New Orleans,LA,70124
John,Watson,54714 Linden Street,Portland,OR,97211
Roy,Powell,9064 Sutherland Court,Sacramento,CA,94207
Carolyn,Garcia,942 Eagan Center,San Diego,CA,92191
Dennis,Mason,71 Delladonna Place,Raleigh,NC,27626
Jeffrey,Hart,334 Tennyson Alley,Omaha,NE,68110
Howard,Sullivan,8915 Northwestern Junction,Brockton,MA,02305
Louise,Garza,568 Kim Alley,Los Angeles,CA,90081
Mary,Howell,85835 Harbort Junction,New Brunswick,NJ,08922
Nicole,Simmons,971 Katie Way,Washington,DC,20409
Carol,Boyd,477 Forest Run Drive,Naperville,IL,60567
Maria,Howard,28206 Messerschmidt Hill,New York City,NY,10280
Lawrence,Stone,40666 Hagan Plaza,Kansas City,MO,64160
Gary,Jones,936 Melvin Parkway,Tucson,AZ,85754
Todd,Andrews,85 Stone Corner Center,Minneapolis,MN,55458
Rose,Bryant,72 Northland Center,Memphis,TN,38188
Phyllis,Mills,5 Maple Wood Court,Washington,DC,20238
Virginia,Bell,4369 Pennsylvania Terrace,Pomona,CA,91797
Rebecca,Phillips,92166 Green Place,Saginaw,MI,48609
Adam,Reynolds,8 Loeprich Pass,Tampa,FL,33615
Charles,Wright,2 Paget Lane,Evansville,IN,47737
Stephen,Williamson,028 Vermont Court,Los Angeles,CA,90076
Dennis,Bell,285 Harper Street,Albany,NY,12242
Joyce,Torres,7 Luster Park,Houston,TX,77212
Frances,Lawrence,3 Lerdahl Circle,College Station,TX,77844
Nicole,Williamson,252 Sheridan Junction,Vancouver,WA,98664
Justin,Graham,84757 Lawn Hill,Inglewood,CA,90398
Robin,Frazier,967 Gale Trail,Spokane,WA,99220
Dorothy,Lawrence,4 Everett Avenue,Billings,MT,59112
Bonnie,Kim,723 Westerfield Drive,Boston,MA,02298
Diane,Payne,11 Jay Trail,Phoenix,AZ,85015
Juan,Spencer,799 Atwood Terrace,Denver,CO,80270
Antonio,Robertson,12789 Anthes Drive,Alpharetta,GA,30022
Patricia,,9 Fairview Drive,Saint Louis,MO,63116
Carolyn,Peterson,3 Reindahl Terrace,Huntsville,TX,77343
Jeremy,Wagner,29782 Farwell Plaza,Wichita,KS,67210`;

    let papaConfig = {
      header: this.state.header,
      skipEmptyLines: this.state.skipEmptyLines,
      complete: this.handleData
    };

    if (this.state.delimiter) {
      papaConfig.delimiter = this.state.delimiter;
    }

    Papa.parse(input, papaConfig);
  }

  handleData(papa) {
    let formattedData;
    if (Array.isArray(papa.data[0])) {
      formattedData = papa.data.map((row) => {
        let obj = {};

        row.forEach((col, index) => {
          let key = `column${index+1}`;
          obj[key] = col;
        });

        return obj;
      });
    } else {
      formattedData = papa.data;
    }

    this.setState({
      originalData: formattedData,
    });
  }

  setDelimiter(e) {
    this.setState({delimiter: e.target.value});
  }

  handleSetHeaders() {
    this.setState({header: !this.state.header});
  }
  handleSetSkipEmptyLines() {
    this.setState({skipEmptyLines: !this.state.skipEmptyLines});
  }
  handleTextInput(e) {
    this.setState({wranglerInput: e.target.value});
  }

  render() {
    return (
      <div className="wrangler-container">
        <h1>Wrangler</h1>

        <div className="wrangler-input row">
          <div className="col-xs-6">
            <h3>Instruction to try out</h3>

            <ol>
              <li>Paste some input data to the input text area</li>
              <li>Specify delimiter and configure options</li>
              <li>Click <strong>Wrangle</strong></li>
              <li>Click on a column header and the list of actions will show up on the left</li>
              <li>Choose any action and play around!</li>
            </ol>
          </div>
          <div className="col-xs-6">
            <h3>Copy Input Text</h3>
            <textarea
              className="form-control"
              onChange={this.handleTextInput}
            />
          </div>
        </div>

        <div className="parse-options">
          <h3>Options</h3>

          <form className="form-inline">
            <div className="delimiter">
              {/* delimiter */}
              <label className="control-label">Delimiter</label>
              <input
                type="text"
                className="form-control"
                onChange={this.setDelimiter}
              />
            </div>

            <div className="checkbox">
              {/* header */}
              <label>
                <input type="checkbox"
                  onChange={this.handleSetHeaders}
                  checked={this.state.headers}
                /> First line as column name?
              </label>
            </div>

            <div className="checkbox">
              {/* skipEmptyLines */}
              <label>
                <input type="checkbox"
                  onChange={this.handleSetSkipEmptyLines}
                /> Skip empty lines?
              </label>
            </div>
          </form>
        </div>

        <br/>
        <div className="text-center">
          <button
            className="btn btn-primary"
            onClick={this.wrangle}
          >
            Wrangle
          </button>
        </div>

        <br/>

        {
          this.state.originalData.length ?
            <WrangleData data={this.state.originalData} />
          :
            null
        }

      </div>
    );
  }
}
