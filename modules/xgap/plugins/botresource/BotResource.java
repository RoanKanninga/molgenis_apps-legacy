/* Date:        May 4, 2011
 * Template:	PluginScreenJavaTemplateGen.java.ftl
 * generator:   org.molgenis.generators.ui.PluginScreenJavaTemplateGen 3.3.3
 * 
 * THIS FILE IS A TEMPLATE. PLEASE EDIT :-)
 */

package plugins.botresource;

import org.molgenis.framework.db.Database;
import org.molgenis.framework.server.MolgenisRequest;
import org.molgenis.framework.ui.PluginModel;
import org.molgenis.framework.ui.ScreenController;

public class BotResource extends PluginModel
{
	public BotResource(String name, ScreenController<?> parent)
	{
		super(name, parent);
	}

	@Override
	public String getViewName()
	{
		return "plugins_botresource_BotResource";
	}

	@Override
	public String getViewTemplate()
	{
		return "plugins/botresource/BotResource.ftl";
	}

	@Override
	public void handleRequest(Database db, MolgenisRequest request)
	{
		// replace example below with yours
		// try
		// {
		// //start database transaction
		// db.beginTx();
		//
		// //get the "__action" parameter from the UI
		// String action = request.getAction();
		//
		// if( action.equals("do_add") )
		// {
		// Experiment e = new Experiment();
		// e.set(request);
		// db.add(e);
		// }
		//
		// //commit all database actions above
		// db.commitTx();
		//
		// } catch(Exception e)
		// {
		// db.rollbackTx();
		// //e.g. show a message in your form
		// }
	}

	@Override
	public void reload(Database db)
	{
		// try
		// {
		// Database db = this.getDatabase();
		// Query q = db.query(Experiment.class);
		// q.like("name", "test");
		// List<Experiment> recentExperiments = q.find();
		//
		// //do something
		// }
		// catch(Exception e)
		// {
		// //...
		// }
	}

}
