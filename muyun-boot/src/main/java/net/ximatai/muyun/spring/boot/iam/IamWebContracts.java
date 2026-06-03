package net.ximatai.muyun.spring.boot.iam;

record IamWebError(String code, int status, String message) {
    static IamWebError badRequest(String message) {
        return new IamWebError("IAM_BAD_REQUEST", 400, message);
    }

    static IamWebError conflict(String message) {
        return new IamWebError("IAM_CONFLICT", 409, message);
    }
}
