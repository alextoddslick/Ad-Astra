package earth.terrarium.adastra.common.registry;

import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ThreadLocal context for passing the registration ID to Properties mixins
 * during item/block construction in ResourcefulLib's registry.
 * Uses a stack to handle nested registrations (e.g. item registration triggering
 * entity type class loading which also registers via the same mixin).
 */
public class RegistryIdContext {
    public static final ThreadLocal<Identifier> CURRENT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Deque<Identifier>> ID_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    public static void pushId(Identifier id) {
        ID_STACK.get().push(id);
        CURRENT_ID.set(id);
    }

    public static void popId() {
        Deque<Identifier> stack = ID_STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            CURRENT_ID.remove();
        } else {
            CURRENT_ID.set(stack.peek());
        }
    }
}
