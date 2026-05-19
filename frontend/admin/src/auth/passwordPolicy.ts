// Mirrors the Cognito user-pool password policy in terraform/cognito.tf.
export const MIN_PASSWORD_LENGTH = 12;
export const PASSWORD_RULE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{12,}$/;
export const PASSWORD_HINT = `${MIN_PASSWORD_LENGTH}+ characters with upper, lower, number, and symbol.`;
