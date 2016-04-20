package com.transcend.nas.common;

/**
 * Created by silverhsu on 16/1/1.
 */
public class TutkCodeID {

    public static String SUCCESS = "201"; // status: 'success'
    public static String AUTH_FAIL = "401"; // status: 'Authentication failure'
    public static String UNKNOWN_AUTH_TOKEN_TYPE = "401";// status: 'Unknow Authentication type'
    public static String AUTHTOKENEXPIRED = "602"; // status: 'AuthToken expired'
    public static String HEADER_FORBIDDEN = "403"; // status: 'Request access forbidden'
    public static String THIRD_PARTY_AUTH_FAIL = "401"; // status: 'Third-Party Authentication failure'
    public static String INVALID_EMAIL_OR_PWD = "401"; // status:'Invalid email or password'
    public static String EMAIL_ALREADY_TAKEN = "603"; // status: 'Email already taken'
    public static String UID_ALREADY_REGISTERED = "603"; // status: 'Uid already registered'
    public static String UID_ALREADY_TAKEN = "603"; // status: 'Uid already taken'
    public static String USER_NOT_FOUND = "604"; // status: 'User not found'
    public static String NAS_NOT_FOUND = "604"; // status: 'Nas not found'
    public static String NASINFO_NOT_FOUND = "604"; // status: 'NasInfo not found'
    public static String NOT_VERIFIED = "601"; // status: 'Account not verified'
    public static String INVALID_VERIFICATIONCODE = "602"; // status: 'Invalid verificationCode'
    public static String VERIFICATIONEXPIRED = "602"; // status: 'Verification code expired'
    public static String NEW_PASSWORD_CONFIRM_ERROR = "605"; // status: 'Password does not match the confirm password'
    public static String INVALID_NEW_PASSWORD = "605"; // status: 'Password should not contain special char'
    public static String INVALID_RESETCODE = "602"; // status: 'Invalid resetToken

}
