package plugins.data;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import org.apache.cxf.binding.corba.wsdl.Array;
import org.apache.poi.hssf.record.formula.Ptg;
import org.molgenis.core.Ontology;
import org.molgenis.core.OntologyTerm;
import org.molgenis.framework.db.Database;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.framework.db.QueryRule;
import org.molgenis.framework.db.QueryRule.Operator;
import org.molgenis.framework.ui.PluginModel;
import org.molgenis.framework.ui.ScreenController;
import org.molgenis.organization.Investigation;
import org.molgenis.pheno.Code;
import org.molgenis.pheno.Measurement;
import org.molgenis.pheno.ObservableFeature;
import org.molgenis.pheno.ObservationElement;
import org.molgenis.pheno.ObservationTarget;
import org.molgenis.pheno.ObservedValue;
import org.molgenis.protocol.Protocol;
import org.molgenis.util.Entity;
import org.molgenis.util.Tuple;

import app.FillMetadata;

import plugins.emptydb.emptyDatabase;




public class ImportExcel extends PluginModel<Entity>
{
	private String Status = "";

	private static final long serialVersionUID = 6149846107377048848L;

	public ImportExcel(String name, ScreenController<?> parent)
	{
		super(name, parent);
	}

	@Override
	public String getViewName()
	{
		return "plugins_data_ImportExcel";
	}

	@Override
	public String getViewTemplate()
	{
		return "plugins/data/ImportExcel.ftl";
	}

	@Override
	public void handleRequest(Database db, Tuple request) throws Exception	{

		if ("ImportDatashaperToPheno".equals(request.getAction())) {

			System.out.println("----------------->");

			System.out.println(db.query(Investigation.class).eq(Investigation.NAME, " DataShaper").count());

			Investigation inv = new Investigation();

			if(db.query(Investigation.class).eq(Investigation.NAME, "DataShaper").count() == 0){

				inv.setName("DataShaper");
				db.add(inv);

			}
			loadDataFromExcel(db, request, inv);

		}

		if ("fillinDatabase".equals(request.getAction())) {

			new emptyDatabase(db, false);
			FillMetadata.fillMetadata(db, false);
			Status = "The database is empty now";
		}

	}
	@SuppressWarnings("unchecked")
	public void loadDataFromExcel(Database db, Tuple request, Investigation inv) throws BiffException, IOException, DatabaseException{

		List<String> ProtocolFeatures = new ArrayList<String>();

		String protocolName = "";

		String themeName = "";

		String groupName = "";

		String measurementName = "";

		boolean MeasurementTemporal = false;

		List<ObservedValue> observedValues  = new ArrayList<ObservedValue>();

		HashMap<String, List> linkProtocolMeasurement = new HashMap<String, List>();

		HashMap<String, List> linkCodeMeasurement = new HashMap<String, List>();

		HashMap<String, String> linkUnitMeasurement = new HashMap<String, String>();

		HashMap<String, List> linkProtocolTheme = new HashMap<String, List>();

		HashMap<String, List> linkThemeGroup = new HashMap<String, List>();

		List<Measurement> addedMeasurements = new ArrayList<Measurement>();

		List<Protocol> addedProtocols = new ArrayList<Protocol>();

		List<Code> addedCodes = new ArrayList<Code>();

		List<ObservedValue> addedObservedValues = new ArrayList<ObservedValue>();

		List<Measurement> measurements = new ArrayList<Measurement>();

		List<Protocol> protocols = new ArrayList<Protocol>();

		List<Protocol> themes = new ArrayList<Protocol>();

		List<Protocol> groups = new ArrayList<Protocol>();

		List<OntologyTerm> ontologyTerms = new ArrayList<OntologyTerm>();

		List<Ontology> ontologies = new ArrayList<Ontology>();

		List<Code> codes = new ArrayList<Code>();

		Measurement mea;

		OntologyTerm ontology_Term;

		Ontology ontology;

		Protocol prot, theme, group;

		Code code;

		File tmpDir = new File(System.getProperty("java.io.tmpdir"));

		File file = new File(tmpDir+ "/DataShaperExcel.xls"); 

		if (file.exists()) {

			System.out.println("The excel file is being imported, please be patient");

			this.setStatus("The excel file is being imported, please be patient");

			Workbook workbook = Workbook.getWorkbook(file); 

			Sheet sheet = workbook.getSheet(0); 

			System.out.println(sheet.getCell(0, 0).getContents());

			int row = sheet.getRows();

			int column = sheet.getColumns();

			Measurement headers [] = new Measurement[column];
			
			System.out.println(row);

			for(int i = 0; i < column; i++){
				Measurement headerMea = new Measurement();
				headerMea.setName(sheet.getCell(i, 0).getContents().replaceAll("'", ""));
				headers[i] = headerMea;
				measurements.add(headerMea);
			}
			
			for (int i = 1; i < row - 1; i++){

				mea = new Measurement();

				ontology_Term = new OntologyTerm();

				ontology = new Ontology();

				code = new Code();

				prot = new Protocol();

				theme = new Protocol();

				group = new Protocol();
				
				boolean WhetherDoubleCheck = false;
				
				for(int j = 0; j < column; j++){
					
					if (j==0) { //group is also a protocol 

						groupName = sheet.getCell(j, i).getContents().replaceAll("'", "");

						group.setName(groupName);

						if(!linkThemeGroup.containsKey(groupName)){

							List groupNames = new ArrayList<String>();

							linkThemeGroup.put(groupName, groupNames);
						}

						if(!groups.contains(group))
							groups.add(group);

					}else if (j==1) { //theme is also a protocol 

						themeName = sheet.getCell(j, i).getContents().replaceAll("'", "");

						theme.setName(themeName);

						if(!linkProtocolTheme.containsKey(themeName)){

							List themeNames = new ArrayList<String>();

							linkProtocolTheme.put(themeName, themeNames);
						}

						if(!themes.contains(theme))
							themes.add(theme);

						List<String> gourpHolder = linkThemeGroup.get(groupName);

						gourpHolder.add(themeName);

						linkThemeGroup.put(groupName, gourpHolder);

					}else if(j == 2){

						protocolName = sheet.getCell(j, i).getContents().replaceAll("'", "");

						if(!linkProtocolMeasurement.containsKey(protocolName)){
							ProtocolFeatures = new ArrayList<String>();
							linkProtocolMeasurement.put(protocolName, ProtocolFeatures);
						}

						if(!protocols.contains(prot))
							protocols.add(prot);

						List<String> tempHolder = linkProtocolTheme.get(themeName);

						tempHolder.add(protocolName);

						linkProtocolTheme.put(themeName, tempHolder);

						prot.setName(sheet.getCell(j, i).getContents().replaceAll("'", ""));

					}else if(j == 3){

						measurementName = sheet.getCell(j, i).getContents();

						ontology_Term.setName(measurementName);

						mea.setName(sheet.getCell(j, i).getContents());

						mea.setOntologyReference_Name(measurementName);

						List<String> temporaryHolder = linkProtocolMeasurement.get(protocolName);

						if(!temporaryHolder.contains(protocolName)){

							temporaryHolder.add(measurementName);

							linkProtocolMeasurement.put(protocolName, temporaryHolder);
						}

					}else if (j == 4){

						mea.setDescription(sheet.getCell(j, i).getContents());

					}else if (j==5) {

						OntologyTerm unit = new OntologyTerm();
						String unitName = sheet.getCell(j,i).getContents().replace(" ", "_").replace("/", "_");
						unit.setName(unitName);

						if (unitName !="" && !ontologyTerms.contains(unit)) {
							ontologyTerms.add(unit);
							linkUnitMeasurement.put(measurementName, unitName);
						}

					}else  if( j == 6) { //is repeatable refers to the measurement  Erik says it's the temporal field of measurement entity in pheno model . 
						
						String tmp = sheet.getCell(j,i).getContents();

						if (tmp == "No") MeasurementTemporal = false;
						else if (tmp =="Yes") MeasurementTemporal = true;

						mea.setTemporal(MeasurementTemporal);


					}else if (j == 7) {

						String variableURIName = sheet.getCell(j,i).getContents();
						ontology_Term.setTermPath(variableURIName);
						String array[] = variableURIName.split("#");
						String ontologyName = array[0];
						ontology.setName(ontologyName); //TODO don`t konw the name yet
						ontology.setOntologyURI(ontologyName);
						ontology_Term.setOntology_Name(ontologyName);
					}

					else if(j == 8 || j == 9){
						
						if(sheet.getCell(j, i).getContents().length() > 0 && sheet.getCell(j, i).getContents() != null){

							String [] codeString = sheet.getCell(j, i).getContents().split("\\|");

							for(int index = 0; index < codeString.length; index++){
								codeString[index] = codeString[index].trim();
							}
							//System.out.println(sheet.getCell(j, i).getContents());
							for(int k = 0; k < codeString.length; k++){

								code.setCode_String(codeString[k]);
								code.setDescription(sheet.getCell(j, i).getContents());

								if(linkCodeMeasurement.containsKey(codeString[k])){
									List<String> featuresCode = linkCodeMeasurement.get(codeString[k]);
									if(!featuresCode.contains(measurementName)){
										featuresCode.add(measurementName);
										linkCodeMeasurement.put(codeString[k], featuresCode);
									}
								}else{
									List<String> featuresCode = new ArrayList<String>();
									featuresCode.add(measurementName);
									linkCodeMeasurement.put(codeString[k], featuresCode);
								}
							}
							if(j == 9)
								code.setIsMissing(true);
							codes.add(code);
						}

					}else if(j == 18){
						
						String format = sheet.getCell(j, i).getContents().replaceAll("'", "");
						
						if(format.equalsIgnoreCase("Categorical")){
							mea.setDataType("code");
						}
						if(format.equalsIgnoreCase("Open")){
							WhetherDoubleCheck = true;
						}
						
					}else if(j == 23){
						
						String Target = sheet.getCell(j, i).getContents().replaceAll("'", "");
						
						if(!Target.equals("") && Target != null){
							if(Target.equalsIgnoreCase("Participant")){
								mea.setTargettypeAllowedForRelation_ClassName("Individual");
							}
						}
						
					}else if(j == 25){
						
						String type = sheet.getCell(j, i).getContents().replaceAll("'", "");
						
						if(WhetherDoubleCheck){
							
							if(type.equalsIgnoreCase("Integer")){
								mea.setDataType("int");
							}
							if(type.equalsIgnoreCase("Text")){
								mea.setDataType("string");
							}
							if(type.equalsIgnoreCase("Decimal")){
								mea.setDataType("decimal");
							}
							if(type.equalsIgnoreCase("Date")){
								mea.setDataType("datetime");
							}
						}
					}else{
						
						String cellValue = sheet.getCell(j, i).getContents().replaceAll("'", "");
						
						if(!cellValue.equals("") && cellValue != null){
							
							ObservedValue ob = new ObservedValue();
							//ob.setValue(cellValue);
							ob.setTarget_Name(mea.getName());
							ob.setFeature_Name(headers[j].getName());
							ob.setValue(cellValue);
							if(!observedValues.contains(ob))
								observedValues.add(ob);
						}
					}
				}
				
				if(!measurements.contains(mea))
					measurements.add(mea);

				if(!ontologyTerms.contains(ontology_Term))
					ontologyTerms.add(ontology_Term);

				if(!ontologies.contains(ontology))
					ontologies.add(ontology);
			}

			for(Measurement measure : measurements){

				if(db.query(Measurement.class).eq(Measurement.NAME, measure.getName()).count() == 0){

					if(!addedMeasurements.contains(measure)){
						addedMeasurements.add(measure);
					}
				}

			}

			for( Protocol proto : protocols){

				if(db.query(Protocol.class).eq(Protocol.NAME, proto.getName()).count() == 0){
					if(!addedProtocols.contains(proto)){

						addedProtocols.add(proto);
					}
				}
			}


			for( Code cod : codes){
				if(db.query(Code.class).eq(Code.CODE_STRING, cod.getCode_String()).count() == 0){
					if(!addedCodes.contains(cod)){
						addedCodes.add(cod);
					}
				}
			}



			for (ObservedValue ov: observedValues) {
				if (!observedValues.contains(ov)) {
					addedObservedValues.add(ov);
				}
			}


			try {

				//System.out.println("Just before ontologies are insertd in db : >>>>" + ontologies);
				//System.out.println("Just before addedObservedValues  are insertd in db : >>>>" + addedObservedValues);

				db.add(ontologies);

				db.add(ontologyTerms);

				//db.add(addedObservedValues);

				//link Unit(ontologyTerm) to measurements 
				for (Measurement m: addedMeasurements) {

					String tmp = linkUnitMeasurement.get(m.getName());

					List<String> unitHolder = new ArrayList<String>();

					unitHolder.add(tmp);

					List<OntologyTerm> ontologyTermsList = db.find(OntologyTerm.class, new QueryRule(OntologyTerm.NAME, Operator.IN, unitHolder));

					for (OntologyTerm ot: ontologyTermsList) {
						m.setUnit_Id(ot.getId());
					}
				}
				
				db.add(addedMeasurements);

				// TEMPORARY FIX FOR MREF RESOLVE FOREIGN KEYS BUG
				for (Protocol p : addedProtocols) {

					if(linkProtocolMeasurement.containsKey(p.getName())){
						List<String> featureNames = linkProtocolMeasurement.get(p.getName());
						List<Measurement> measList = db.find(Measurement.class, new QueryRule(Measurement.NAME, Operator.IN, featureNames));
						List<Integer> measIdList = new ArrayList<Integer>();
						for (Measurement m : measList) {
							measIdList.add(m.getId());
						}
						p.setFeatures_Id(measIdList);

					}
				}

				db.add(addedProtocols);

				for (Protocol p : themes) {

					if(linkProtocolTheme.containsKey(p.getName())){

						List<String> subProtocolNames = linkProtocolTheme.get(p.getName());
						List<Protocol> subProtocols = db.find(Protocol.class, new QueryRule(Protocol.NAME, Operator.IN, subProtocolNames));
						List<Integer> subProtocolsId = new ArrayList<Integer>();
						for(Protocol subP : subProtocols){
							subProtocolsId.add(subP.getId());
						}
						p.setSubprotocols_Id(subProtocolsId);
					}
				}

				db.add(themes);

				for (Protocol p : groups) {

					if(linkThemeGroup.containsKey(p.getName())){

						List<String> subProtocolNames = linkThemeGroup.get(p.getName());
						List<Protocol> subProtocols = db.find(Protocol.class, new QueryRule(Protocol.NAME, Operator.IN, subProtocolNames));
						List<Integer> subProtocolsId = new ArrayList<Integer>();
						for(Protocol subP : subProtocols){
							subProtocolsId.add(subP.getId());
						}
						p.setSubprotocols_Id(subProtocolsId);
					}
				}

				db.add(groups);

				for (Code c : addedCodes){
					List<String> featureNames = linkCodeMeasurement.get(c.getCode_String());
					List<Measurement> measList = db.find(Measurement.class, new QueryRule(Measurement.NAME, Operator.IN, featureNames));
					List<Integer> meaIdList = new ArrayList<Integer>();
					for(Measurement m : measList){
						meaIdList.add(m.getId());
					}
					c.setFeature_Id(meaIdList);
				}

				db.add(addedCodes);
				
//				for(ObservedValue ob : observedValues){
//					
//					List<String> featureNames = new ArrayList<String>();
//					
//					featureNames.add(ob.getFeature_Name());
//					
//					List<String> targetNames = new ArrayList<String>();
//					
//					targetNames.add(ob.getTarget_Name());
//					
//					List<Measurement> features = db.find(Measurement.class, new QueryRule(Measurement.NAME, Operator.IN, featureNames));
//					
//					List<Measurement> targets = db.find(Measurement.class, new QueryRule(Measurement.NAME, Operator.IN, targetNames));
//					
//					System.out.println("The feature name is " + featureNames);
//					
//					System.out.println("The target name is " + targetNames);
//					
//					System.out.println(ob.getValue());
//					
//					System.out.println(ob);
//					
//					ob.setFeature_Id(features.get(0).getId());
//					
//					ob.setTarget_Id(targets.get(0).getId());
//					
//					db.add(ob);
//					
//				}
				
				db.add(observedValues);

			} catch (DatabaseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("The file" + tmpDir + " was imported successfully");
			this.setStatus("The file" + tmpDir + " was imported successfully");
		} else {
			System.out.println("The excel file should be located here :"+ tmpDir + " and the name of the file should be DataShaperExcel.xls");
			this.setStatus("The excel file should be located here :"+ tmpDir + " and the name of the file should be DataShaperExcel.xls");

		}

	}
	@Override
	public void reload(Database db)	{
	}


	public void setStatus(String status) {
		Status = status;
	}


	public String getStatus() {
		return Status;
	}
}