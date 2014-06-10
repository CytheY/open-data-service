/*  Open Data Service
    Copyright (C) 2013  Tsysin Konstantin, Reischl Patrick

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
 */
package org.jvalue.ods.server.restlet;

import java.io.IOException;
import java.util.List;

import org.jvalue.ods.db.DbAccessor;
import org.jvalue.ods.logger.Logging;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The Class ClassObjectRestlet.
 */
public class ExecuteQueryRestlet extends Restlet {

	private final DbAccessor<JsonNode> dbAccessor;
	private final String designDocId;
	private final String viewName;
	private final boolean fetchAllDbEntries;
	private final String errorMsg;
	private final String attributeName;
	private final ObjectMapper mapper = new ObjectMapper();


	private ExecuteQueryRestlet(
			DbAccessor<JsonNode> dbAccessor,
			String designDocId, 
			String viewName,
			boolean fetchAllDbEntries,
			String errorMsg,
			String attributeName) {

		this.dbAccessor = dbAccessor;
		this.designDocId = designDocId;
		this.viewName = viewName;
		this.fetchAllDbEntries = fetchAllDbEntries;
		this.errorMsg = errorMsg;
		this.attributeName = attributeName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.Restlet#handle(org.restlet.Request,
	 * org.restlet.Response)
	 */
	@Override
	public void handle(Request request, Response response) {
		String message =  null;
		try {
			try {
				dbAccessor.connect();

				String attributeValue = null;
				if (attributeName != null) 
					attributeValue = request.getAttributes().get(attributeName).toString();

				List<JsonNode> nodes = dbAccessor.executeDocumentQuery(
						designDocId, 
						viewName, 
						attributeValue);

				if (fetchAllDbEntries) message = mapper.writeValueAsString(nodes);
				else message = mapper.writeValueAsString(nodes.get(0));

			} catch (RuntimeException e) {
				String errorMessage = "Could not retrieve data from db: " + e;
				Logging.error(this.getClass(), errorMessage);
				System.err.println(errorMessage);
				message = errorMsg;
			}

		} catch (IOException e) {
			String errorMessage = "Error during client request: " + e;
			Logging.error(this.getClass(), errorMessage);
			System.err.println(errorMessage);
		}

		response.setEntity(message, MediaType.APPLICATION_JSON);
	}


	public static final class Builder {
		
		private final DbAccessor<JsonNode> dbAccessor;
		private final String designDocId, viewName;
		private boolean fetchAllDbEntries = true;
		private String errorMsg = "Could not retrieve data.";
		private String attributeName = null;

		public Builder(DbAccessor<JsonNode> dbAccessor, String designDocId, String viewName) {
			if (dbAccessor == null || designDocId == null || viewName == null)
				throw new NullPointerException("params cannot be null");
			this.dbAccessor = dbAccessor;
			this.designDocId = designDocId;
			this.viewName = viewName;
		}


		/** Whether all entries should be fetched from the db. If false only
		* the first entry will be fetched. */
		public Builder fetchAllDbEntries(boolean fetchAllDbEntries) {
			this.fetchAllDbEntries = fetchAllDbEntries;
			return this;
		}

		public Builder errorMsg(String customErrorMsg) {
			if (errorMsg == null) throw new NullPointerException("param cannot be null");
			this.errorMsg = customErrorMsg;
			return this;
		}

		/** Set this value if you wish to specify a attribute value to be used during querying. */
		public Builder attributeName(String attributeName) {
			this.attributeName = attributeName;
			return this;
		}

		public ExecuteQueryRestlet build() {
			return new ExecuteQueryRestlet(
					dbAccessor,
					designDocId,
					viewName,
					fetchAllDbEntries,
					errorMsg,
					attributeName);
		}
	}
}