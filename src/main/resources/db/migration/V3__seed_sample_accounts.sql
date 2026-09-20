INSERT INTO accounts (account_number, balance)
SELECT 'A100', 5000.00
WHERE NOT EXISTS (
    SELECT 1 FROM accounts WHERE account_number = 'A100'
);

INSERT INTO accounts (account_number, balance)
SELECT 'A200', 1000.00
WHERE NOT EXISTS (
    SELECT 1 FROM accounts WHERE account_number = 'A200'
);
