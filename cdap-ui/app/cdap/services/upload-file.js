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

import Rx from 'rx';
const UploadFile = ({url, fileContents, headers}) => {
  let observerable$ = Rx.Observable.create(function(observer) {
    let xhr = new window.XMLHttpRequest();
    let path;
    xhr.upload.addEventListener('progress', function (e) {
      if (e.type === 'progress') {
        console.info(`File Upload to '${url}' in progress`);
      }
    });
    path = url;
    xhr.open('POST', path, true);
    if (typeof headers === 'object') {
      Object.keys(headers)
        .forEach(header => xhr.setRequestHeader(header, headers[header]));
    }
    xhr.send(fileContents);
    xhr.onreadystatechange = function () {
      if (xhr.readyState === 4) {
        if (xhr.status > 399){
          observer.onError(xhr.response);
        } else {
          observer.onNext(true);
        }
      }
    };
  });
  return observerable$;
};

export default UploadFile;
