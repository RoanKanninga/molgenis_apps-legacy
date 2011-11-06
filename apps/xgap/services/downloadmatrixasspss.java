package services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.List;

import matrix.DataMatrixInstance;
import matrix.general.DataMatrixHandler;

import org.molgenis.data.Data;
import org.molgenis.framework.db.Database;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.framework.db.QueryRule;
import org.molgenis.framework.db.QueryRule.Operator;
import org.molgenis.framework.server.MolgenisContext;
import org.molgenis.framework.server.MolgenisRequest;
import org.molgenis.framework.server.MolgenisResponse;
import org.molgenis.framework.server.MolgenisService;
import org.molgenis.util.HttpServletRequestTuple;
import org.molgenis.util.Tuple;

import plugins.matrix.manager.Browser;

public class downloadmatrixasspss implements MolgenisService {

	private MolgenisContext mc;
	
	public downloadmatrixasspss(MolgenisContext mc)
	{
		this.mc = mc;
	}
	
	@Override
	public void handleRequest(MolgenisRequest request, MolgenisResponse response) throws ParseException,
			DatabaseException, IOException
	{

		Tuple req = null;
		boolean databaseIsAvailable = false;
		boolean setupSuccess = false;
		boolean argumentsAreCorrect = false;
		
		Database db = null;
		DataMatrixInstance instance = null;

		try {
			db = request.getDatabase();
			databaseIsAvailable = true;
		} catch (Exception e) {
			PrintWriter out = response.getResponse().getWriter();
			response.getResponse().setContentType("text/plain");
			out.print("Database unavailable.");
			out.print("\n\n");
			e.printStackTrace(out);
			out.close();
		}

		if (databaseIsAvailable) {
			try {
				req = new HttpServletRequestTuple(request.getRequest());
				
				//special exception for filtered content: get matrix instance from memory and do complete handle
				if(req.getString("id").equals("inmemory"))
				{
					OutputStream outSpecial = response.getResponse().getOutputStream();
					File spssFile = Browser.inmemory.getAsSpssFile();
					URL localURL = spssFile.toURI().toURL();
					URLConnection conn = localURL.openConnection();
					InputStream in = new BufferedInputStream(conn.getInputStream());
					response.getResponse().setContentType("application/spss");
					response.getResponse().setContentLength((int) spssFile.length());
					response.getResponse().setHeader("Content-disposition","attachment; filename=\""+Browser.inmemory.getData().getName()+"_"+"some"+".sav"+"\"");
					byte[] buffer = new byte[2048];
					for (;;) {
						int nBytes = in.read(buffer);
						if (nBytes <= 0)
							break;
						outSpecial.write(buffer, 0, nBytes);
					}
					outSpecial.flush();
					outSpecial.close();
					return;
				}
				
				int matrixId = req.getInt("id");
				QueryRule q = new QueryRule("id", Operator.EQUALS, matrixId);
				List<Data> dataList = db.find(Data.class, q);
				if (dataList.size() != 1) {
					throw new Exception("Datamatrix for ID " + matrixId
							+ " was not found.");
				}
				Data data = dataList.get(0);
				DataMatrixHandler dmh = new DataMatrixHandler(db);
				instance = dmh.createInstance(data);
				setupSuccess = true;
			} catch (Exception e) {
				PrintWriter out = response.getResponse().getWriter();
				response.getResponse().setContentType("text/plain");
				displayUsage(out, db);
				out.print("\n\n");
				e.printStackTrace(out);
				out.close();
			}
		}
		
		if(setupSuccess){
			try {
				if (req.getString("download").equals("all"))
				{
					//correct
				}else if (req.getString("download").equals("some"))
				{
					req.getInt("coff");
					req.getInt("clim");
					req.getInt("roff");
					req.getInt("rlim");
					//correct
				}else{
					throw new Exception("Bad arguments.");
				}
				argumentsAreCorrect = true;
			}catch (Exception e) {
				PrintWriter out = response.getResponse().getWriter();
				response.getResponse().setContentType("text/plain");
				displayUsage(out, db);
				out.print("\n\n");
				e.printStackTrace(out);
				out.close();
			}
		}

		if (argumentsAreCorrect) {
			OutputStream outFile = response.getResponse().getOutputStream();
			try {
				File spssFile = null;
				String download = req.getString("download");
				if (download.equals("all"))
				{
					spssFile = instance.getAsSpssFile();
				}else if (download.equals("some"))
				{
					int colOffset = req.getInt("coff");
					int colLimit = req.getInt("clim");
					int rowOffset = req.getInt("roff");
					int rowLimit = req.getInt("rlim");
					spssFile = instance.getSubMatrixByOffset(rowOffset, rowLimit, colOffset, colLimit).getAsSpssFile();
				}
				URL localURL = spssFile.toURI().toURL();
				URLConnection conn = localURL.openConnection();
				InputStream in = new BufferedInputStream(conn.getInputStream());
				response.getResponse().setContentType("application/spss");
				response.getResponse().setContentLength((int) spssFile.length());
				response.getResponse().setHeader("Content-disposition","attachment; filename=\""+instance.getData().getName()+"_"+download+".sav"+"\"");
				byte[] buffer = new byte[2048];
				for (;;) {
					int nBytes = in.read(buffer);
					if (nBytes <= 0)
						break;
					outFile.write(buffer, 0, nBytes);
				}
				outFile.flush();
			} catch (Exception e) {
				e.printStackTrace();
				//logger.error(e);
			} finally {
				outFile.close();
			}
		}
	}

	public void displayUsage(PrintWriter out, Database db) {
		String usage = "Potentially downloadable matrices available in this database:\n\n" + matricesFromDb(db) + "\n";
		out.print(usage);
	}

	public String matricesFromDb(Database db) {
		String res = "";
		try {
			List<Data> dataList = db.find(Data.class);
//			MolgenisFileHandler mfh = new MolgenisFileHandler(db);
			for (Data data : dataList) {
//				try{
//					mfh.findFile(data).equals("null")
//				}
				
					res += data.toString() + "\n";
				
			}
		} catch (Exception e) {
			res += "An error occurred when retrieving matrix information:\n\n";
			res += e.getMessage();
		}
		return res;
	}

}