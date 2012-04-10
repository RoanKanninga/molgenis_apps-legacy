package org.molgenis.lifelines.listeners;

import javax.persistence.EntityManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.zip.DataFormatException;

import org.molgenis.framework.db.Database;
import org.molgenis.organization.Investigation;
import org.molgenis.pheno.Category;
import org.molgenis.pheno.Measurement;
import org.molgenis.pheno.ObservableFeature;
import org.molgenis.protocol.Protocol;
import org.molgenis.util.CsvFileReader;
import org.molgenis.util.Tuple;
import static ch.lambdaj.Lambda.*;
import static org.hamcrest.Matchers.*;

//import static org.hamcrest.text.IsEqualIgnoringCase.*;

public class CategoryLoader {
	
	private final EntityManager em;
	private final Map<String, Protocol> protocols;
	private final List<Category> categories = new ArrayList<Category>();
	private final Investigation investigation;
	private final boolean shareMeasurements;
	private List<String> protocolsToImport;
	private final CsvFileReader csvFileReader;
	
	public CategoryLoader(final File file, final String encoding, Map<String, Protocol> protocols,
			Investigation investigation, String name, EntityManager em,
			boolean shareMeasurements, List<String> protocolsToImport) throws IOException, DataFormatException {
		this.em = em;
		this.protocols = protocols;
		this.investigation = investigation;
		this.shareMeasurements = shareMeasurements;
		
		this.protocolsToImport = protocolsToImport;
		
		this.csvFileReader = new CsvFileReader(file, encoding);
	}

	final private HashSet<String> uniqueLabelWithinCode = new HashSet<String>();
	
	public void load() throws Exception {
		final Iterator<Tuple> iterator = csvFileReader.iterator();
		while(iterator.hasNext()) {
			final Tuple tuple = iterator.next();
		
			String tableName = tuple.getString("TABNAAM");
			if(protocolsToImport != null) {
				if(!protocolsToImport.contains(tableName.toUpperCase())) {
					return;
				}
			}
			
			final Protocol protocol = protocols.get(tableName);
			if(protocol == null) {
				return;
			}
	
			String fieldName = tuple.getString("VELD");
			if (!shareMeasurements) {
				fieldName = tableName + "_" + tuple.getString("VELD");
			}
	
			final List<ObservableFeature> measurements = (List<ObservableFeature>) protocol.getFeatures();
			final List<ObservableFeature> filterMeasurements = filter(
					having(on(Measurement.class).getName(),
							equalToIgnoringCase(fieldName)), measurements);
			final Measurement measurement = (Measurement) filterMeasurements.get(0);
	
			final Category category = new Category();
			category.setInvestigation(investigation);
			category.setCode_String(tuple.getString("VALLABELVAL"));
			category.setLabel(tuple.getString("VALLABELABEL"));
			category.setDescription(tuple.getString("VALLABELABEL"));
			category.getCategoriesMeasurementCollection().add(measurement); // add to
			//measurement.getCategories(db)
	
			measurement.getCategories().add(category); // measurment
			
			
			category.setName(fieldName + "_" + category.getLabel());
			category.setName(fieldName + "_" + tuple.getString("VALLABELVAL"));
	
			//check to see if label is Unique
			final String uniqueLabel = fieldName + "_" + category.getLabel(); 
			if(uniqueLabelWithinCode.contains(uniqueLabel)) {
				throw new IllegalArgumentException(String.format("Non unique in category Label: %s", uniqueLabel));
			}
			uniqueLabelWithinCode.add(uniqueLabel);
			
			categories.add(category);
		}
		commit();
	}

	
	private void commit() throws Exception {
		try {
			em.getTransaction().begin();
			for (Category c : categories) {
				em.persist(c);
			}
			em.flush();
			em.getTransaction().commit();
		} catch (Exception ex) {
			throw ex;
		}
	}
}