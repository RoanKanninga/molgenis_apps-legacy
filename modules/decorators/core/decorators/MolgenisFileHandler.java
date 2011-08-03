package decorators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.molgenis.core.MolgenisFile;
import org.molgenis.framework.db.Database;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.framework.db.QueryRule;
import org.molgenis.framework.db.QueryRule.Operator;
import org.molgenis.framework.db.jdbc.JDBCFileSourceHelper;
import org.molgenis.util.ValueLabel;

import app.servlet.MolgenisServlet;

import decorators.NameConvention;

public class MolgenisFileHandler extends JDBCFileSourceHelper
{
	
	public MolgenisFileHandler(Database db)
	{
		super(db);
		setVariantId(MolgenisServlet.getMolgenisVariantID());
	}

	/**
	 * Get the storage directory for this MolgenisFile object. Directory is
	 * created if it does not exist, and will throw error if this fails.
	 * 
	 * @param mf
	 * @return
	 * @throws Exception
	 */
	public File getStorageDirFor(MolgenisFile mf) throws Exception
	{
		return getStorageDirFor(mf.get__Type().toLowerCase());
	}

	/**
	 * Get the storage directory for a type of file denoted by this String.
	 * Directory is created if it does not exist, and will throw error if this
	 * fails.
	 * 
	 * @param type
	 * @return
	 * @throws Exception 
	 * @throws Exception
	 */
	public File getStorageDirFor(String type) throws Exception
	{
		// lowercase 'type' for directory usage
		type = type.toLowerCase();

		// create new MolgenisFile and get all subclasses (ie. file types)
		MolgenisFile mf = new MolgenisFile();
		List<String> values = new ArrayList<String>();
		for (ValueLabel vl : mf.get__TypeOptions())
		{
			values.add(vl.getValue().toString().toLowerCase());
		}

		// check if the type is a known subclass
		if (!values.contains(type))
		{
			throw new TypeUnknownException("MolgenisFile type '" + type + "' not known");
		}

		// create pointer to storage location, try to mkdir if not exists
		File storageDir = getFilesource();
		File typeStorageDir = new File(storageDir.getAbsolutePath() + File.separator + type);
		if (!typeStorageDir.exists())
		{
			boolean createSuccess = typeStorageDir.mkdirs();
			if (!createSuccess)
			{
				throw new IOException("Creation of storage dir '" + typeStorageDir + "' failed");
			}
		}
		return typeStorageDir;
	}

	/**
	 * Deletes the file that belongs to this MolgenisFile entity. Use only if
	 * you know what you're doing, because you usually want the decorator to
	 * take care of deleting the file by removing the MolgenisFile.
	 * 
	 * @param mf
	 * @throws Exception 
	 */
	public void deleteFile(MolgenisFile mf) throws Exception
	{
		File theFile = getFile(mf);
		if (!theFile.exists())
		{
			throw new FileNotFoundException("No file found for name '" + mf.getName() + "'");
		}
		// try to delete
		System.gc(); // HACK FOR MS WINDOWS
		boolean success = theFile.delete();
		if (!success)
		{
			// try again after 100 millisec
			Thread.sleep(100);
			System.gc(); // HACK FOR MS WINDOWS
			success = theFile.delete();
			if (!success)
			{
				// give up :(
				throw new DatabaseException("SEVERE: Deletion of " + theFile.getAbsolutePath() + " failed.");
			}
		}
	}

	/**
	 * Get the file pointer for this MolgenisFile, if it exists.
	 * 
	 * @param db
	 * @param mf
	 * @return
	 * @throws Exception 
	 */
	public File getFile(MolgenisFile mf) throws Exception
	{
		File typeStorage = getStorageDirFor(mf.get__Type().toLowerCase());
		File dataSource = new File(typeStorage.getAbsolutePath() + File.separator
				+ NameConvention.escapeFileName(mf.getName()) + "." + mf.getExtension());
		if (!dataSource.exists())
		{
			throw new FileNotFoundException("No file found for name '" + mf.getName() + "'");
		}
		return dataSource;

	}

	/**
	 * Get the file pointer for this name of a MolgenisFile, if it exists.
	 * 
	 * @param name
	 * @return
	 * @throws Exception 
	 */
	public File getFile(String name) throws Exception
	{
		QueryRule q = new QueryRule("name", Operator.EQUALS, name);
		List<MolgenisFile> mfList = getDb().find(MolgenisFile.class, q);

		if (mfList.size() == 0)
		{
			throw new FileNotFoundException("No MolgenisFile for name '" + name + "' found.");
		}
		else
		{
			return getFile(mfList.get(0));
		}
	}

	/**
	 * Find the type of MolgenisFile (subclass) for this file, if it exists.
	 * 
	 * @param db
	 * @param mf
	 * @return
	 * @throws Exception 
	 * @throws Exception
	 */
	public String findFile(MolgenisFile mf) throws Exception
	{
		// get all possible options and iterate through the possible directories
		List<ValueLabel> fileTypes = mf.get__TypeOptions();
		for (ValueLabel ft : fileTypes)
		{
			// can also do label in this case
			String fileType = ft.getValue().toString().toLowerCase();
			File typeStorage = getStorageDirFor(fileType);
			// make file pointer
			File theFile = new File(typeStorage.getAbsolutePath() + File.separator
					+ NameConvention.escapeFileName(mf.getName()));
			if (theFile.exists())
			{
				// return if file pointer denotes a real file
				return fileType;
			}
		}
		throw new FileNotFoundException("No file found for name '" + mf.getName() + "'");
	}

	public class TypeUnknownException extends Exception
	{
		private static final long serialVersionUID = 7026641720938935426L;

		public TypeUnknownException(String e)
		{
			super(e);
		}
	}

}