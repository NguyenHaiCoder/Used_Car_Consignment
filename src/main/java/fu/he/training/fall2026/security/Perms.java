package fu.he.training.fall2026.security;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Nguyễn Việt Hải: tra {@link Perm} theo mã lưu trong DB, mã lạ (đã bỏ khỏi danh mục) trả null. */
public final class Perms {

    private static final Map<String, Perm> BY_CODE = Arrays.stream(Perm.values())
            .collect(Collectors.toUnmodifiableMap(Enum::name, Function.identity()));

    private Perms() {
    }

    public static Perm byCode(String code) {
        return code == null ? null : BY_CODE.get(code);
    }
}
