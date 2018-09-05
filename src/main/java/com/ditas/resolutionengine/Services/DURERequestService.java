package com.ditas.resolutionengine.Services;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DURERequestService {
	
	@Autowired
	private RepositoryRequestService repositoryService;
    
    @Value("${dure.blueprints}")
    private String dureBlueprintsPath;

	public String createRequest(String elasticResponse, JSONObject app_requirements) {
		
		//Get the JSON response from Elastic Search for further parsing
		JSONObject elastic_response_json = new JSONObject(elasticResponse);
		
		
		//Parse the number of returned blueprints from Elastic Search response
		int num_of_hits = Integer.parseInt(elastic_response_json.getJSONObject("hits").get("total").toString());
		
		//Here will be stored all the ids of the returned blueprints in order to retrieve them from the Blueprint Repository
		ArrayList<String> idsList = new ArrayList<String>();
		
		//Here will be stored all the method names that had their tags matched (during the Elastic Search search) for its blueprint
		HashMap<Integer, ArrayList<String>> methodsList = new HashMap<Integer, ArrayList<String>>();
		
		JSONObject esResult;	
		for (int bp_index = 0 ; bp_index < num_of_hits ; bp_index++) {
			
			esResult = elastic_response_json.getJSONObject("hits").getJSONArray("hits").getJSONObject(bp_index);
			idsList.add(esResult.get("_id").toString());
			
			JSONObject result_innerHits =  esResult.getJSONObject("inner_hits").getJSONObject("tags").getJSONObject("hits");
			
			//Parse the number of matched methods of the current blueprint from Elastic Search response
			int num_of_matched_methods = Integer.parseInt(result_innerHits.get("total").toString());
			ArrayList<String> list_of_methods = new ArrayList<String>();
			for (int method_index = 0 ; method_index < num_of_matched_methods ; method_index++) {
				String method =  result_innerHits.getJSONArray("hits").getJSONObject(method_index).getJSONObject("_source").get("method_id").toString();
				list_of_methods.add(method);		
			}
			methodsList.put(bp_index, list_of_methods);		
			
		}
		
		for (int i = 0; i < num_of_hits ; i++) {
			System.out.println(idsList.get(i));
			for (String s: methodsList.get(i)) {
				System.out.println(s);
			}
		}
		
		//Fetch the blueprints from repository and store them in a list
		ArrayList<JSONObject> blueprints = repositoryService.fetchFromRepository(idsList);		
		String dure_request = buildDURERequest(blueprints, methodsList, app_requirements);
		
		
		return dure_request;
		
	}
	
	public String buildDURERequest(ArrayList<JSONObject> blueprints, HashMap<Integer, ArrayList<String>> methodsList, JSONObject app_requirements) {
		JSONObject dure_request_json = new JSONObject();
		
		//JSONObject requirements = new JSONObject(app_requirements);
		
		ArrayList<Object> list = new ArrayList<Object>();
		for (int i = 0; i < blueprints.size() ; i++) {
			JSONObject json = new JSONObject();
			json.put("blueprint", blueprints.get(i));
			json.put("methodNames", methodsList.get(i));
			list.add(json);
		}
		dure_request_json.put("applicationRequirements", app_requirements);
		dure_request_json.put("candidates", list);
		
		
		return dure_request_json.toString();
	}
	
	
	

	public String sendRequest(String dureRequest) {
		// TODO Auto-generated method stub
		
		URL url;
		
		try {
			
			url = new URL("http://"+dureBlueprintsPath);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			
			OutputStream os = conn.getOutputStream();
			os.write(dureRequest.getBytes());
			os.flush();
			

			BufferedReader br = new BufferedReader(new InputStreamReader(
					(conn.getInputStream())));

			String output;
			String response = "";
			System.out.println("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				response += output+"\n";
			}

			conn.disconnect();
			
			return response;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		return null;
	}
}