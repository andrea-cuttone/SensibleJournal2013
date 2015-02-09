package dk.dtu.imm.sensible.components;

public interface ISelectableTab {
	
	public void onTabSelected();
	public void onTabUnselected();
	public String getTabTitle();
}
