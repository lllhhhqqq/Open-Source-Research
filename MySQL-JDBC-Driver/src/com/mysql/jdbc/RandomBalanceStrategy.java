/*
 Copyright  2007 MySQL AB, 2008-2010 Sun Microsystems
 All rights reserved. Use is subject to license terms.

  The MySQL Connector/J is licensed under the terms of the GPL,
  like most MySQL Connectors. There are special exceptions to the
  terms and conditions of the GPL as it is applied to this software,
  see the FLOSS License Exception available on mysql.com.

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License as
  published by the Free Software Foundation; version 2 of the
  License.

  This program is distributed in the hope that it will be useful,  
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  02110-1301 USA
 
 */
package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class RandomBalanceStrategy implements BalanceStrategy {
	private static my.Debug DEBUG = new my.Debug(my.Debug.Driver);//我加上的

	public RandomBalanceStrategy() {
	}

	public void destroy() {
		// we don't have anything to clean up
	}

	public void init(Connection conn, Properties props) throws SQLException {
		// we don't have anything to initialize
	}

	public ConnectionImpl pickConnection(LoadBalancingConnectionProxy proxy,
			List<String> configuredHosts, Map<String, ConnectionImpl> liveConnections, long[] responseTimes,
			int numRetries) throws SQLException {
		try {//我加上的
		DEBUG.P(this,"pickConnection(...)");
		DEBUG.P("configuredHosts="+configuredHosts);
		DEBUG.P("liveConnections="+liveConnections);
		DEBUG.PA("responseTimes",responseTimes);
		DEBUG.P("numRetries="+numRetries);

		int numHosts = configuredHosts.size();

		SQLException ex = null;

		//白名单
		List<String> whiteList = new ArrayList<String>(numHosts);
		whiteList.addAll(configuredHosts);
		
		//黑名单，对应的主机发生过异常
		Map<String, Long> blackList = proxy.getGlobalBlacklist();

		DEBUG.P("whiteList="+whiteList);
		DEBUG.P("blackList="+blackList);

		whiteList.removeAll(blackList.keySet());
		
		Map<String, Integer> whiteListMap = this.getArrayIndexMap(whiteList);
		
		DEBUG.P("whiteListMap="+whiteListMap);

		for (int attempts = 0; attempts < numRetries;) {
			//如1.9、1.1取1.0
			//random取0,1,2,whiteList.size()-1
			int random = (int) Math.floor((Math.random() * whiteList.size()));

			DEBUG.P("random="+random);

			if(whiteList.size() == 0){
				throw SQLError.createSQLException("No hosts configured", null);
			}

			String hostPortSpec = whiteList.get(random);

			ConnectionImpl conn = liveConnections.get(hostPortSpec);

			DEBUG.P("hostPortSpec="+hostPortSpec);
			DEBUG.P("conn="+conn);

			if (conn == null) {
				try {
					conn = proxy.createConnectionForHost(hostPortSpec);
				} catch (SQLException sqlEx) {
					ex = sqlEx;

					if (proxy.shouldExceptionTriggerFailover(sqlEx)) {

						Integer whiteListIndex = whiteListMap.get(hostPortSpec);

						// exclude this host from being picked again
						if (whiteListIndex != null) {
							whiteList.remove(whiteListIndex.intValue());
							whiteListMap = this.getArrayIndexMap(whiteList);
						}
						proxy.addToGlobalBlacklist( hostPortSpec );

						if (whiteList.size() == 0) {
							attempts++;
							try {
								Thread.sleep(250);
							} catch (InterruptedException e) {
							}

							// start fresh
							whiteListMap = new HashMap<String, Integer>(numHosts);
							whiteList.addAll(configuredHosts);
							blackList = proxy.getGlobalBlacklist();

							whiteList.removeAll(blackList.keySet());
							whiteListMap = this.getArrayIndexMap(whiteList);
						}

						continue;
					} else {
						throw sqlEx;
					}
				}
			}
			
			return conn;
		}

		if (ex != null) {
			throw ex;
		}

		return null; // we won't get here, compiler can't tell

		}finally{//我加上的
		DEBUG.P(0,this,"pickConnection(...)");
		}
	}
	
	private Map<String, Integer> getArrayIndexMap(List<String> l) {
		Map<String, Integer> m = new HashMap<String, Integer>(l.size());
		for (int i = 0; i < l.size(); i++) {
			m.put(l.get(i), new Integer(i));
		}
		return m;
		
	}

}