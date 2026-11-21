package com.seckill.common.exception;

import com.seckill.common.result.R;
import com.seckill.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理：HTTP 状态码与 body.code 一致，bizCode 保留业务细分。
 *
 * 例：
 * - 参数校验失败：HTTP 400 + code 400
 * - 售罄/重复购买/活动结束：HTTP 409 + code 409 + bizCode 1001/1002/1003
 * - 未预期异常：HTTP 500 + code 500
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBiz(BizException e) {
        int status = mapHttpStatus(e.getCode());
        Integer bizCode = isBizCode(e.getCode()) ? e.getCode() : null;
        log.warn("业务异常[code={}, http={}, bizCode={}]: {}", e.getCode(), status, bizCode, e.getMessage());
        return ResponseEntity.status(status).body(R.fail(status, bizCode, e.getMessage()));
    }

    /** @RequestBody 上的 @Valid 校验失败 → 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("；"));
        return badRequest(msg);
    }

    /** 表单/Query 参数绑定校验失败 → 400 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("；"));
        return badRequest(msg);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<R<Void>> handleBadRequest(Exception e) {
        log.warn("请求参数错误: {}", e.getMessage());
        return badRequest("请求参数格式错误");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(R.fail(ResultCode.METHOD_NOT_ALLOWED, "请求方法不支持: " + e.getMethod()));
    }

    /** 上传文件超过大小限制 → 413 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<R<Void>> handleUpload(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(R.fail(ResultCode.PAYLOAD_TOO_LARGE, "文件大小超出限制（最大 10MB）"));
    }

    /** 唯一索引冲突：用户名重复、并发重复下单等 → 409 */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<R<Void>> handleDuplicate(DuplicateKeyException e) {
        log.warn("唯一约束冲突: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(R.fail(ResultCode.CONFLICT, "数据已存在，请勿重复提交"));
    }

    /** 静态资源/接口不存在 → 404 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(R.fail(ResultCode.NOT_FOUND, "接口或资源不存在"));
    }

    /** 未预期异常 → 500，堆栈只写日志，不返回前端 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleOther(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(ResultCode.ERROR, "系统繁忙，请稍后再试"));
    }

    private ResponseEntity<R<Void>> badRequest(String message) {
        return ResponseEntity.badRequest()
                .body(R.fail(ResultCode.BAD_REQUEST, message));
    }

    /**
     * 业务细分码 -> HTTP 状态：
     * 1001 售罄 / 1002 重复购买 / 1003 活动结束 → 409
     * 400~599 直接作为 HTTP 状态
     * 其他未知业务码 → 500
     */
    private int mapHttpStatus(int code) {
        if (code == ResultCode.SOLD_OUT
                || code == ResultCode.REPEAT_BUY
                || code == ResultCode.ACTIVITY_CLOSED) {
            return HttpStatus.CONFLICT.value();
        }
        if (code >= 400 && code <= 599) {
            return code;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    /** 只有非 HTTP 范围的业务码才放进 bizCode */
    private boolean isBizCode(int code) {
        return code >= 1000;
    }
}
