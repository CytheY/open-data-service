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
package org.jvalue.ods.filter;

import org.jvalue.ods.data.DataSource;
import org.jvalue.ods.data.DataSourceVisitor;


public final class FilterVisitorAdapter<P,R> implements Filter<P,R> {
	
	private final DataSourceVisitor<P,R> visitor;

	public FilterVisitorAdapter(DataSourceVisitor<P,R> visitor) {
		if (visitor == null) throw new NullPointerException("param cannot be null");
		this.visitor = visitor;
	}

	
	@Override
	public R filter(DataSource source, P param) {
		return source.accept(visitor, param);
	}

}