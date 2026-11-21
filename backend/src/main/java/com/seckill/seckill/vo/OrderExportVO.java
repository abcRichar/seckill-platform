package com.seckill.seckill.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * EasyExcel 导出对象：注解即表头
 */
@Data
@ColumnWidth(18)
public class OrderExportVO {

    @ExcelProperty("订单号")
    @ColumnWidth(30)
    private String orderNo;

    @ExcelProperty("用户ID")
    private String userId;

    @ExcelProperty("商品名称")
    @ColumnWidth(28)
    private String productName;

    @ExcelProperty("数量")
    private Integer quantity;

    @ExcelProperty("秒杀价(元)")
    private String seckillPrice;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("退款状态")
    private String refundStatus;

    @ExcelProperty("下单时间")
    @ColumnWidth(22)
    private String createTime;

    @ExcelProperty("支付时间")
    @ColumnWidth(22)
    private String payTime;
}
