package com.stericson.RootTools;

public class Permissions {
	String type;
	String user;
	String group;
	String other;
	int permissions;

	public String getType()
	{
		return type;
	}
	
	public int getPermissions()
	{
		return this.permissions;
	}
	
    public String getUserPermissions()
    {
    	return this.user;
    }
    
    public String getGroupPermissions()
    {
    	return this.group;
    }
    
    public String getOtherPermissions()
    {
    	return this.other;
    }
    
    public void setType(String type)
    {
    	this.type = type;
    }
    
    public void setPermissions(int permissions)
    {
    	this.permissions = permissions;
    }
    
    public void setUserPermissions(String user)
    {
    	this.user = user;
    }
    
    public void setGroupPermissions(String group)
    {
    	this.group = group;
    }
    
    public void setOtherPermissions(String other)
    {
    	this.other = other;
    }
}
