package org.molgenis.auth.service.persontouser;

import java.util.List;

import org.molgenis.auth.MolgenisGroup;
import org.molgenis.auth.Person;

public class PersonToUserModel
{

	List<Person> personList;
	List<MolgenisGroup> groupList;

	public List<Person> getPersonList()
	{
		return personList;
	}

	public void setPersonList(List<Person> personList)
	{
		this.personList = personList;
	}

	public List<MolgenisGroup> getGroupList()
	{
		return groupList;
	}

	public void setGroupList(List<MolgenisGroup> groupList)
	{
		this.groupList = groupList;
	}

}
