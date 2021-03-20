package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.api.computer.IEnanReactorController;
import cr0s.warpdrive.block.TileEntityAbstractEnergyCoreOrController;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.Collections;

import net.minecraft.tileentity.TileEntityType;

public class TileEntityEnanReactorController extends TileEntityAbstractEnergyCoreOrController implements IEnanReactorController {
	
	public static TileEntityType<TileEntityEnanReactorController> TYPE;
	
	// persistent properties
	// (none)
	
	// computed properties
	// (none)
	
	// @TODO implement reactor controller
	private WeakReference<TileEntityEnanReactorCore> tileEntityEnanReactorCoreWeakReference = null;
	
	public TileEntityEnanReactorController() {
		this(TYPE);
	}
	
	public TileEntityEnanReactorController(@Nonnull final TileEntityType<? extends TileEntityEnanReactorController> tileEntityType) {
		super(tileEntityType);
		
		peripheralName = "warpdriveEnanReactorController";
		addMethods(new String[] {
				"getInstabilities",
				"instabilityTarget",
				"outputMode",
				"stabilizerEnergy",
				"state"
		});
		CC_scripts = Collections.singletonList("startup");
	}
	
	@Override
	protected void doUpdateParameters(final boolean isDirty) {
		// no operation
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getEnergyRequired() {
		return new Object[] { false, "No energy consumption" };
	}
	
	@Override
	public Object[] getLocalPosition() {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return null;
		}
		return tileEntityEnanReactorCore.getLocalPosition();
	}
	
	@Override
	public Object[] getAssemblyStatus() {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Object[] { false, "No core detected" };
		}
		return tileEntityEnanReactorCore.getAssemblyStatus();
	}
	
	@Override
	public String[] name(@Nonnull final Object[] arguments) {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return super.name(new Object[0]); // return current local values
		}
		return tileEntityEnanReactorCore.name(arguments);
	}
	
	@Override
	public Double[] getInstabilities() {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Double[] { -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D,
			                      -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D };
		}
		return tileEntityEnanReactorCore.getInstabilities();
	}
	
	@Override
	public Double[] instabilityTarget(@Nonnull final Object[] arguments) {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Double[] { -1.0D };
		}
		return tileEntityEnanReactorCore.instabilityTarget(arguments);
	}
	
	@Override
	public Object[] outputMode(@Nonnull final Object[] arguments) {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Object[] { "???", -1, "Core not found" };
		}
		return tileEntityEnanReactorCore.outputMode(arguments);
	}
	
	@Override
	public Object[] stabilizerEnergy(@Nonnull final Object[] arguments) {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Object[] { -1, "Core not found" };
		}
		return tileEntityEnanReactorCore.stabilizerEnergy(arguments);
	}
	
	@Override
	public Object[] state() {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Object[] { -1, "Core not found" };
		}
		return tileEntityEnanReactorCore.state();
	}
	
	@Override
	public Object[] energyDisplayUnits(@Nonnull final Object[] arguments) {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return null;
		}
		return tileEntityEnanReactorCore.energyDisplayUnits(arguments);
	}
	
	@Override
	public Object[] getEnergyStatus() {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return null;
		}
		return tileEntityEnanReactorCore.getEnergyStatus();
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	public Object[] getInstabilities(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getInstabilities();
	}
	
	@Callback(direct = true)
	public Object[] instabilityTarget(final Context context, final Arguments arguments) {
		return instabilityTarget(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] outputMode(final Context context, final Arguments arguments) {
		return outputMode(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] stabilizerEnergy(final Context context, final Arguments arguments) {
		return stabilizerEnergy(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] state(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return state();
	}
	
	// ComputerCraft IDynamicPeripheral methods
	@Override
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "getInstabilities":
			return getInstabilities();
			
		case "instabilityTarget":
			return instabilityTarget(arguments);
			
		case "outputMode":
			return outputMode(arguments);
			
		case "stabilizerEnergy":
			return stabilizerEnergy(arguments);
			
		case "state":
			return state();
			
		default:
			return super.CC_callMethod(methodName, arguments);
		}
	}
}
