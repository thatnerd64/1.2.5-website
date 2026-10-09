package cpw.mods.fml.common.registry;

import com.google.common.base.Throwables;
import cpw.mods.fml.common.FMLLog;
import java.lang.reflect.Field;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import org.apache.logging.log4j.Level;

/**
 * Replacement for Forge's ObjectHolderRef. The original unlocks {@code static final} fields through
 * sun.reflect.ReflectionFactory; a compiled page can simply assign them.
 */
class ObjectHolderRef {
    private Field field;
    private String injectedObject;
    private boolean isBlock;
    private boolean isItem;

    ObjectHolderRef(Field field, String injectedObject, boolean extractFromExistingValues) {
        this.field = field;
        this.isBlock = Block.class.isAssignableFrom(field.getType());
        this.isItem = Item.class.isAssignableFrom(field.getType());
        if (extractFromExistingValues) {
            try {
                Object existing = field.get(null);
                if (existing == null || existing == GameData.getBlockRegistry().getDefaultValue()) {
                    this.injectedObject = null;
                    this.field = null;
                    this.isBlock = false;
                    this.isItem = false;
                    return;
                }
                this.injectedObject = this.isBlock ? GameData.getBlockRegistry().func_148750_c(existing)
                        : (this.isItem ? GameData.getItemRegistry().func_148750_c(existing) : null);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        } else {
            this.injectedObject = injectedObject;
        }
        if (this.injectedObject == null || !this.isValid()) {
            throw new IllegalStateException(String.format(
                    "The ObjectHolder annotation cannot apply to a field that is not an Item or Block (found : %s at %s.%s)",
                    field.getType().getName(), field.getClass().getName(), field.getName()));
        }
    }

    public boolean isValid() {
        return this.isBlock || this.isItem;
    }

    public void apply() {
        Object thing;
        if (this.isBlock) {
            thing = GameData.getBlockRegistry().func_82594_a(this.injectedObject);
            if (thing == Blocks.field_150350_a) {
                thing = null;
            }
        } else {
            thing = this.isItem ? GameData.getItemRegistry().func_82594_a(this.injectedObject) : null;
        }
        if (thing == null) {
            FMLLog.getLogger().log(Level.DEBUG,
                    "Unable to lookup {} for {}. This means the object wasn't registered. It's likely just mod options.",
                    new Object[] {this.injectedObject, this.field});
            return;
        }
        try {
            this.field.set(null, thing);
        } catch (Exception e) {
            FMLLog.log(Level.WARN, e, "Unable to set %s with value %s (%s)", new Object[] {this.field, thing,
                    this.injectedObject});
        }
    }
}
