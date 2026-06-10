package com.eventhub.admin.merchant;

import com.eventhub.user.MerchantRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MerchantAdminMapper {

    @Select("""
            SELECT merchant.id, merchant.name, merchant.description, merchant.status,
                   COUNT(staff.user_id) AS staff_count, merchant.created_at
            FROM eh_merchant merchant
            LEFT JOIN eh_merchant_staff staff
              ON staff.merchant_id = merchant.id AND staff.status = 'ACTIVE'
            GROUP BY merchant.id
            ORDER BY merchant.created_at DESC, merchant.id DESC
            """)
    List<MerchantView> findAll();

    @Select("SELECT COUNT(*) FROM eh_merchant WHERE id = #{merchantId}")
    int countById(long merchantId);

    @Insert("""
            INSERT INTO eh_merchant (name, description, status)
            VALUES (#{name}, #{description}, 'ACTIVE')
            """)
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(MerchantRecord merchant);

    @Update("UPDATE eh_merchant SET status = #{status} WHERE id = #{merchantId}")
    int updateStatus(@Param("merchantId") long merchantId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM eh_merchant_staff WHERE user_id = #{userId}")
    int countUserBinding(long userId);

    @Insert("""
            INSERT INTO eh_merchant_staff (merchant_id, user_id, staff_role, status)
            VALUES (#{merchantId}, #{userId}, 'OPERATOR', 'ACTIVE')
            """)
    void bindStaff(@Param("merchantId") long merchantId, @Param("userId") long userId);
}
