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
 * Web Service Implementation that translates Token Saml to SIR request
 * @author iinigo
 */


import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import javax.jws.WebParam;
import javax.jws.WebService;

import eu.stork.peps.auth.commons.PersonalAttribute;
import eu.stork.peps.auth.commons.PersonalAttributeList;
import eu.stork.peps.auth.commons.STORKAuthnRequest;
import eu.stork.peps.auth.commons.STORKAuthnResponse;
import eu.stork.peps.auth.engine.STORKSAMLEngine;
import eu.stork.peps.exceptions.STORKSAMLEngineException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebService
public class StorkSamlEngineWS {
			 	
	 	private static final Logger LOG = LoggerFactory.getLogger(StorkSamlEngineWS.class.getName());	 
	 	
		private static STORKSAMLEngine engine;	
		
		private static Properties configs; 
		
		/**
         * Generates SAML Token
         * @destination destination URL
         * @serviceProvider Service Provider's name
         * @qaal qaa level requested
         * @personalAttributeList attribute list requestesd in JSON format
         * @return SAML token in XML format
         */     
        public byte[] generateSTORKAuthnRequest(
        		@WebParam(name="destination") String destination, 
        		@WebParam(name="serviceProvider") String serviceProvider, 
        		@WebParam(name="qaal") int qaal, 
        		@WebParam(name="urlAssertionConsumerService") String urlAssertionConsumerService,
        		@WebParam(name="spId") String spId,
        		@WebParam(name="spApplication") String spApplication,
        		@WebParam(name="personalAttributeList") String personalAttributeList) {

        	//Load properties file
        	configs = new Properties();
    		try {
				configs.load(this.getClass().getClassLoader().getResourceAsStream(Constants.WS_PROPERTIES));
			} catch (IOException e) {
				LOG.error("Reading properties file: " + e.getMessage());
				e.printStackTrace();
				throw new STORKSAMLEngineWSException(e.getMessage());
			}
    		
        	
        	//Unmarshall JSON chain 	
        	JSONObject json;
        	
        	try {
				json = new JSONObject(personalAttributeList);
			} catch (ParseException e) {
				LOG.error("Generating STORKAuthnRequest: " + e.getMessage());
				e.printStackTrace();
				throw new STORKSAMLEngineWSException(e.getMessage());
			}
			
        	Iterator<?> itr = json.keys();
        	PersonalAttributeList pAttList = new PersonalAttributeList();
        	
        	 while(itr.hasNext()){
        		String attributeName = (String) itr.next();
        		PersonalAttribute pAttribute = new PersonalAttribute();
        		pAttribute.setName(attributeName);
        		if(json.get(attributeName).getClass().getName().equals("org.json.JSONArray")){
        			ArrayList<String> attributeValues = new ArrayList<String>();
        			JSONArray jarray = (JSONArray) json.get(attributeName);
        			for(int i=0; i<jarray.length();i++){
        				if(i==0){
        					if(jarray.getString(i).equals("true"))
        						pAttribute.setIsRequired(true);
        					else 
        						pAttribute.setIsRequired(false);
        				}else{
        					attributeValues.add(jarray.getString(i));
        			}
        			pAttribute.setValue(attributeValues);
        			}
        		}        		
        		else if(json.get(attributeName).equals("true"))
        			pAttribute.setIsRequired(true);
				else 
					pAttribute.setIsRequired(false);
				pAttList.add(pAttribute);        			 
        	 }   
        	 
        	 
        	STORKAuthnRequest authnRequest = new STORKAuthnRequest();
     		
     		authnRequest.setDestination(destination);
     		authnRequest.setProviderName(serviceProvider);	
     		authnRequest.setQaa(qaal);
     		authnRequest.setPersonalAttributeList(pAttList);
     		authnRequest.setAssertionConsumerServiceURL(urlAssertionConsumerService);     		   		
    		
    		//new parameters
    		authnRequest.setSpSector(configs.getProperty(Constants.SP_SECTOR));
    		authnRequest.setSpInstitution(serviceProvider);
    		authnRequest.setSpApplication(spApplication);
    		authnRequest.setSpCountry(configs.getProperty(Constants.SP_COUNTRY));
    		
    		//V-IDP parameters
    		authnRequest.setSPID(spId);
     		
     		
			//Generates Stork Request
    					
			try {					      	     		
				engine = STORKSAMLEngine.getInstance("WS");
				authnRequest = engine.generateSTORKAuthnRequest(authnRequest);
				engine.validateSTORKAuthnRequest(authnRequest.getTokenSaml());
				LOG.info("Generated STORKAuthnRequest(id: {})", authnRequest.getSamlId());	
			}catch (STORKSAMLEngineException e) {	
				LOG.error("Generating STORKAuthnRequest: " + e.getMessage());
				e.printStackTrace();
				throw new STORKSAMLEngineWSException(e.getMessage());
			}
					
			return authnRequest.getTokenSaml();
        }
        
        /**
         * Validates and translates SAML Tokens
         * @response SAML Token
         * @userIP citizen's IP
         * @return attribute list in JSON format
         */     
        public String validateSTORKAuthnResponse(
        		@WebParam(name="response") byte[] response, 
        		@WebParam(name="userIP")String userIP) {
        	
        	//Validate response
			STORKAuthnResponse tokenSaml = null;
						
			try {
				tokenSaml = engine.validateSTORKAuthnResponse(response, userIP);
				LOG.info("Validated STORKAuthnResponse(id: {})", tokenSaml.getSamlId());
			} catch (STORKSAMLEngineException e) {
				LOG.error("Validating STORKAuthnResponse: " + e.getMessage());
				e.printStackTrace();
				throw new STORKSAMLEngineWSException(e.getMessage());
			}
		
    		PersonalAttributeList pAttList = (PersonalAttributeList) tokenSaml.getPersonalAttributeList();
    		JSONObject json = new JSONObject();
    		
    		if(pAttList != null){    	
    		
    			Iterator<?> itr  = pAttList.iterator();
    		
    			while(itr.hasNext()){
    				PersonalAttribute att = (PersonalAttribute) itr.next();
    				String attName = att.getName();
    			 	if(pAttList.get(attName).getValue().size() > 1){
    			 		JSONArray jarray = new JSONArray();
    			 		Iterator<?> iter = pAttList.get(attName).getValue().iterator();
    			 		while(iter.hasNext()){
    			 			jarray.put(iter.next());
    			 		}
    			 		json.put(attName, jarray);
    			 	}
    			 	else if(pAttList.get(attName).getValue().size()== 1){
    			 		json.put(attName, pAttList.get(attName).getValue().get(0));
    			 	}else{
    			 		json.put(attName, "");
    			 	}
         		}    		
    		    return json.toString();
    		}else{
    			return null;
    		}
        }
}