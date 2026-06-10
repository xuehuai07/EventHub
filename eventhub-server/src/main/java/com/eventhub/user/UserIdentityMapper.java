package com.eventhub.user;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserIdentityMapper {

    @Select("""
            SELECT id, username, phone, password_hash, display_name, status
            FROM eh_user
            WHERE username = #{identifier} OR phone = #{identifier}
            LIMIT 1
            """)
    UserRecord findByIdentifier(String identifier);

    @Select("""
            SELECT id, username, phone, password_hash, display_name, status
            FROM eh_user
            WHERE id = #{id}
            """)
    UserRecord findById(long id);

    @Select("""
            SELECT COUNT(*)
            FROM eh_user
            WHERE (#{username} IS NOT NULL AND username = #{username})
               OR (#{phone} IS NOT NULL AND phone = #{phone})
            """)
    int countByIdentifiers(@Param("username") String username, @Param("phone") String phone);

    @Insert("""
            INSERT INTO eh_user (username, phone, password_hash, display_name)
            VALUES (#{username}, #{phone}, #{passwordHash}, #{displayName})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(UserRecord user);

    @Insert("""
            INSERT INTO eh_user_role (user_id, role_id)
            SELECT #{userId}, id FROM eh_role WHERE code = #{roleCode}
            """)
    void assignRole(@Param("userId") long userId, @Param("roleCode") String roleCode);

    @Insert("""
            INSERT IGNORE INTO eh_user_role (user_id, role_id)
            SELECT #{userId}, id FROM eh_role WHERE code = #{roleCode}
            """)
    void assignRoleIfMissing(@Param("userId") long userId, @Param("roleCode") String roleCode);

    @Select("""
            SELECT role.code
            FROM eh_role role
            JOIN eh_user_role user_role ON user_role.role_id = role.id
            WHERE user_role.user_id = #{userId}
            ORDER BY role.code
            """)
    List<String> findRoles(long userId);

    @Select("""
            SELECT DISTINCT permission.code
            FROM eh_permission permission
            JOIN eh_role_permission role_permission ON role_permission.permission_id = permission.id
            JOIN eh_user_role user_role ON user_role.role_id = role_permission.role_id
            WHERE user_role.user_id = #{userId}
            ORDER BY permission.code
            """)
    List<String> findPermissions(long userId);

    @Select("""
            SELECT merchant.id AS merchant_id,
                   merchant.name AS merchant_name,
                   merchant.status AS merchant_status,
                   staff.status AS staff_status
            FROM eh_merchant_staff staff
            JOIN eh_merchant merchant ON merchant.id = staff.merchant_id
            WHERE staff.user_id = #{userId}
            """)
    MerchantBinding findMerchantBinding(long userId);

    @Insert("""
            INSERT INTO eh_merchant (name, description, status)
            VALUES (#{name}, #{description}, 'ACTIVE')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertMerchant(MerchantRecord merchant);

    @Insert("""
            INSERT INTO eh_merchant_staff (merchant_id, user_id, staff_role, status)
            VALUES (#{merchantId}, #{userId}, 'OWNER', 'ACTIVE')
            """)
    void bindMerchantStaff(@Param("merchantId") long merchantId, @Param("userId") long userId);
}
