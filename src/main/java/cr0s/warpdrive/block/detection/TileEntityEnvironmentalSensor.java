package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.block.TileEntityAbstractMachine;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.StateAir;
import cr0s.warpdrive.event.ChunkHandler;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.Set;

import net.minecraft.block.state.IBlockState;
import net.minecraft.world.biome.Biome;

import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.fml.common.Optional;

public class TileEntityEnvironmentalSensor extends TileEntityAbstractMachine {
	
	// persistent properties
	// (none)
	
	// computed properties
	private int tickUpdate;
	private int airConcentration;
	
	public TileEntityEnvironmentalSensor() {
		super();
		
		peripheralName = "warpdriveEnvironmentalSensor";
		addMethods(new String[] {
			"getAtmosphere",
			"getBiome",
			"getHumidity",
			"getTemperature",
			"getWeather",
			"getWorldTime"
		});
		CC_scripts = Collections.singletonList("clock");
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		tickUpdate--;
		if (tickUpdate >= 0) {
			return;
		}
		tickUpdate = WarpDriveConfig.G_PARAMETERS_UPDATE_INTERVAL_TICKS;
		
		final IBlockState blockState = world.getBlockState(pos);
		updateBlockState(blockState, BlockProperties.ACTIVE, isEnabled);
		
		// update breathable state from main thread
		final CelestialObject celestialObject = CelestialObjectManager.get(world, pos.getX(), pos.getZ());
		final boolean hasAtmosphere = celestialObject != null && celestialObject.hasAtmosphere();
		if (hasAtmosphere) {
			airConcentration = 32;
		} else {
			final StateAir stateAir = ChunkHandler.getStateAir(world, pos.getX(), pos.getY(), pos.getZ());
			airConcentration = stateAir == null ? 0 : stateAir.concentration;
		}
	}
	
	// Common OC/CC methods
	public Object[] getAtmosphere() {
		if (!isEnabled) {
			return new Object[] { false, "Sensor is disabled." };
		}
		
		return new Object[] { true, airConcentration > 0, airConcentration };
	}
	
	public Object[] getBiome() {
		if (!isEnabled) {
			return new Object[] { false, "Sensor is disabled." };
		}
		
		final Biome biome = world.getBiome(pos);
		final Set<Type> types = BiomeDictionary.getTypes(biome);
		final Object[] result = new Object[2 + types.size()];
		result[0] = true;
		result[1] = biome.biomeName;
		int index = 2;
		for (final Type type : types) {
			result[index] = type.getName();
			index++;
		}
		return result;
	}
	
	public Object[] getHumidity() {
		if (!isEnabled) {
			return new Object[] { false, "Sensor is disabled." };
		}
		
		// note: we don't account for surroundings and current weather or day time.
		final Biome biome = world.getBiome(pos);
		final float rainfall = biome.getRainfall();
		return new Object[] { true, rainfall > 0.85F ? "WET" : rainfall < 0.15F ? "DRY" : "MEDIUM", rainfall };
	}
	
	public Object[] getTemperature() {
		if (!isEnabled) {
			return new Object[] { false, "Sensor is disabled." };
		}
		
		// note: we don't account for surroundings and current day time.
		final Biome biome = world.getBiome(pos);
		return new Object[] { true, biome.getTempCategory().name(), biome.getTemperature(pos) };
	}
	
	private boolean isSnowNotRain() {
		final Biome biome = world.getBiome(pos);
		if (biome.getEnableSnow()) {
			return true;
		}
		// note: base game is a bit inconsistent. We follow the snow particles rule as this is closer to weather/atmosphere.
		// - snow layer are placed when it's raining and rawTemperature is below 0.8
		// - snow particles are rendered instead of rain when correctedTemperature is below 0.15
		// - cauldron is filling when rawTemperature is above 0.15
		// - water freeze when rawTemperature is below 0.15, light below 10 and not surrounded by 4 water blocks
		final int precipitationHeight = world.getPrecipitationHeight(pos).getY();
		final float rawTemperature = biome.getTemperature(pos);
		final float correctedTemperature = world.getBiomeProvider().getTemperatureAtHeight(rawTemperature, precipitationHeight);
		return correctedTemperature < 0.15F;
	}
	
	public Object[] getWeather() {
		if (!isEnabled) {
			return new Object[] { false, "Sensor is disabled." };
		}
		
		// note: we return estimated time in seconds as it's more natural. Also the smooth transition adds a bit of delay anyway.
		if (world.isThundering()) {
			if (isSnowNotRain()) {
				return new Object[] { true, "BLIZZARD", world.getWorldInfo().getThunderTime() / 20 };
			} else {
				return new Object[] { true, "THUNDER", world.getWorldInfo().getThunderTime() / 20 };
			}
		} else if (world.isRaining()) {
			if (isSnowNotRain()) {
				return new Object[] { true, "SNOW", world.getWorldInfo().getRainTime() / 20 };
			} else {
				return new Object[] { true, "RAIN", world.getWorldInfo().getRainTime() / 20 };
			}
		}
		return new Object[] { true, "CLEAR", world.getWorldInfo().getCleanWeatherTime() / 20 };
	}
	
	public Object[] getWorldTime() {
		if (!isEnabled) {
			return new Object[] { false, "Sensor is disabled." };
		}
		
		// returns the current day, hour of the day, minutes of the day, and number of seconds simulated in the world (or play time for single player).
		// note: we return simulated seconds as it's more natural and discourages continuous pooling/abuse in LUA.
		final int day = (int) ((6000L + world.getWorldTime()) / 24000L);
		final int dayTime = 2400 * (int) ((6000L + world.getWorldTime()) % 24000L) / 24000;
		return new Object[] { true, day, dayTime / 100, (dayTime % 100) * 60 / 100, world.getTotalWorldTime() / 20 };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getAtmosphere(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getAtmosphere();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getBiome(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getBiome();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getHumidity(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getHumidity();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getTemperature(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getTemperature();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getWeather(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getWeather();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getWorldTime(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getWorldTime();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "getAtmosphere":
			return getAtmosphere();
		case "getBiome":
			return getBiome();
		case "getHumidity":
			return getHumidity();
		case "getTemperature":
			return getTemperature();
		case "getWeather":
			return getWeather();
		case "getWorldTime":
			return getWorldTime();
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
}