/* 
 * Licensed under the EUPL, Version 1.1 as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence. You may
 * obtain a copy of the Licence at:
 * 
 * http://www.osor.eu/eupl/european-union-public-licence-eupl-v.1.1
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * Licence for the specific language governing permissions and limitations under
 * the Licence.
 */

package es.stork.ws;

/**
 * The EndpointPublisher class
 * 
 * @author iinigo
 */


import javax.xml.ws.Endpoint;

public class EndpointPublisher {
	public static void main(String[] args) throws Exception {
		System.out.println("Starting Server");
		StorkSamlEngineWS implementor = new StorkSamlEngineWS();
		String address = "http://localhost:9000/StorkSamlEngineWS";
		Endpoint.publish(address, implementor);

	}
}
