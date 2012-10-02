/*
 * Copyright 2010-2012 Luca Garulli (l.garulli--at--orientechnologies.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.server.network.protocol.http.command.post;

import java.util.List;

import com.orientechnologies.orient.core.command.OCommandManager;
import com.orientechnologies.orient.core.command.OCommandRequestText;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.server.db.OSharedDocumentDatabase;
import com.orientechnologies.orient.server.network.protocol.http.OHttpRequest;
import com.orientechnologies.orient.server.network.protocol.http.OHttpResponse;
import com.orientechnologies.orient.server.network.protocol.http.OHttpUtils;
import com.orientechnologies.orient.server.network.protocol.http.command.OServerCommandAuthenticatedDbAbstract;

public class OServerCommandPostCommand extends OServerCommandAuthenticatedDbAbstract {
  private static final String[] NAMES = { "POST|command/*" };

  @Override
  @SuppressWarnings("unchecked")
  public boolean execute(final OHttpRequest iRequest, OHttpResponse iResponse) throws Exception {
    final String[] urlParts = checkSyntax(iRequest.url, 3, "Syntax error: command/<database>/<language>/<command-text>[/limit]");

    // TRY TO GET THE COMMAND FROM THE URL, THEN FROM THE CONTENT
    final String language = urlParts.length > 2 ? urlParts[2].trim() : "sql";
    final String text = urlParts.length > 3 ? urlParts[3].trim() : iRequest.content;
    final int limit = urlParts.length > 4 ? Integer.parseInt(urlParts[4].trim()) : -1;

    iRequest.data.commandInfo = "Command";
    iRequest.data.commandDetail = text;

    ODatabaseDocumentTx db = null;

    final Object response;

    try {
      db = getProfiledDatabaseInstance(iRequest);

      final OCommandRequestText cmd = (OCommandRequestText) OCommandManager.instance().getRequester(language);
      cmd.setText(text);
      cmd.setLimit(limit);
      response = db.command(cmd).execute();

    } finally {
      if (db != null)
        OSharedDocumentDatabase.release(db);
    }

    if (response instanceof List<?>)
      iResponse.writeRecords((List<OIdentifiable>) response);
    else if (response == null || response instanceof Integer)
      iResponse.send(OHttpUtils.STATUS_OK_CODE, "OK", OHttpUtils.CONTENT_TEXT_PLAIN, response, null);
    else if (response instanceof ODocument)
      iResponse.send(OHttpUtils.STATUS_OK_CODE, "OK", OHttpUtils.CONTENT_TEXT_PLAIN, ((ODocument) response).toJSON(), null);
    else
      iResponse.send(OHttpUtils.STATUS_OK_CODE, "OK", OHttpUtils.CONTENT_TEXT_PLAIN, response.toString(), null);
    return false;
  }

  @Override
  public String[] getNames() {
    return NAMES;
  }
}
