package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.api.IStringSerializable;

import java.util.Random;

public class StructureGroup implements IStringSerializable {
	
	protected String group;
	protected String name;
	
	public StructureGroup(final String group, final String name) {
		this.group = group;
		this.name = name;
	}
	
	@Override
	public String getName() {
		return group + ":" + (name.isEmpty() || name.isEmpty() ? "*" : name);
	}
	
	public AbstractStructureInstance instantiate(Random random) {
		if (group.equals("asteroidField")) {
			return new AsteroidFieldInstance(null, random);
		}
		return StructureManager.getStructure(random, group, name).instantiate(random);
	}
	
	public String getGroup() {
		return group;
	}
	
	@Override
	public String toString() {
		return String.format("StructureGroup %s", getName());
	}
}
