use capitalx_db;

INSERT INTO customers (customer_code, customer_name, created_at)
VALUES 
('CUST001', 'Ravi Kumar', NOW()),
('CUST002', 'Anita Sharma', NOW());

INSERT INTO assets (asset_code, asset_name, asset_type, exchange_or_market, created_at)
VALUES
('TCS', 'Tata Consultancy Services', 'STOCK', 'NSE', NOW()),
('INFY', 'Infosys Ltd', 'STOCK', 'NSE', NOW()),
('BTC', 'Bitcoin', 'CRYPTO', 'BINANCE', NOW()),
('GOLD', 'Gold ETF', 'COMMODITY', 'MCX', NOW());

-- Ravi Kumar - FY 2025 Upload
INSERT INTO portfolio_uploads (customer_id, period_type, period_label, upload_date, file_name)
VALUES
(1, 'ANNUAL', 'FY-2025', NOW(), 'Ravi_FY2025.xlsx');

-- Anita Sharma - FY 2025 Upload
INSERT INTO portfolio_uploads (customer_id, period_type, period_label, upload_date, file_name)
VALUES
(2, 'ANNUAL', 'FY-2025', NOW(), 'Anita_FY2025.xlsx');


INSERT INTO portfolio_holdings
(upload_id, customer_id, asset_id, quantity, buy_price, current_price,
 invested_value, current_value, profit_loss,
 investment_start_date, investment_end_date)
VALUES
(1, 1, 1, 100, 3200.00, 3500.00, 320000.00, 350000.00, 30000.00, '2024-04-01', NULL), -- TCS
(1, 1, 3, 0.5, 2000000.00, 2400000.00, 1000000.00, 1200000.00, 200000.00, '2024-06-15', NULL); -- BTC


INSERT INTO portfolio_holdings
(upload_id, customer_id, asset_id, quantity, buy_price, current_price,
 invested_value, current_value, profit_loss,
 investment_start_date, investment_end_date)
VALUES
(2, 2, 2, 150, 1400.00, 1350.00, 210000.00, 202500.00, -7500.00, '2024-05-10', NULL), -- INFY
(2, 2, 4, 50, 58000.00, 62000.00, 2900000.00, 3100000.00, 200000.00, '2024-08-01', NULL); -- GOLD

INSERT INTO portfolio_summary
(upload_id, customer_id, total_invested_value, total_current_value,
 total_profit_loss, number_of_assets, number_of_profitable_assets, number_of_loss_assets)
VALUES
(1, 1, 1320000.00, 1550000.00, 230000.00, 2, 2, 0);

INSERT INTO portfolio_summary
(upload_id, customer_id, total_invested_value, total_current_value,
 total_profit_loss, number_of_assets, number_of_profitable_assets, number_of_loss_assets)
VALUES
(2, 2, 3110000.00, 3302500.00, 192500.00, 2, 1, 1);

INSERT INTO annual_performance
(customer_id, financial_year, opening_value, closing_value,
 total_invested_during_year, total_profit_loss,
 best_performing_asset, worst_performing_asset)
VALUES
(1, 2025, 0.00, 1550000.00, 1320000.00, 230000.00, 'BTC', 'TCS'),
(2, 2025, 0.00, 3302500.00, 3110000.00, 192500.00, 'GOLD', 'INFY');
