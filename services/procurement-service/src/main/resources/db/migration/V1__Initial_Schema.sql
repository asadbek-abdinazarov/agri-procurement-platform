-- V1__Initial_Schema.sql
-- Create procurements table
CREATE TABLE procurements (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    quantity_amount NUMERIC(19, 4) NOT NULL,
    quantity_unit VARCHAR(50) NOT NULL,
    budget_amount NUMERIC(19, 4) NOT NULL,
    budget_currency VARCHAR(3) NOT NULL,
    deadline TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    buyer_id VARCHAR(36) NOT NULL,
    awarded_bid_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create bids table
CREATE TABLE bids (
    id VARCHAR(36) PRIMARY KEY,
    procurement_id VARCHAR(36) NOT NULL,
    vendor_id VARCHAR(36) NOT NULL,
    bid_amount NUMERIC(19, 4) NOT NULL,
    bid_currency VARCHAR(3) NOT NULL,
    bid_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_bids_procurement FOREIGN KEY (procurement_id) REFERENCES procurements(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_procurements_buyer_id ON procurements(buyer_id);
CREATE INDEX idx_procurements_status ON procurements(status);
CREATE INDEX idx_procurements_deadline ON procurements(deadline);
CREATE INDEX idx_procurements_status_deadline ON procurements(status, deadline);
CREATE INDEX idx_bids_procurement_id ON bids(procurement_id);
CREATE INDEX idx_bids_vendor_id ON bids(vendor_id);
CREATE INDEX idx_bids_status ON bids(status);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for updated_at
CREATE TRIGGER update_procurements_updated_at
    BEFORE UPDATE ON procurements
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_bids_updated_at
    BEFORE UPDATE ON bids
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE procurements IS 'Agricultural procurement opportunities';
COMMENT ON TABLE bids IS 'Vendor bids on procurement opportunities';
COMMENT ON COLUMN procurements.status IS 'Status: DRAFT, PUBLISHED, BIDDING_OPEN, BIDDING_CLOSED, AWARDED, CANCELLED';
COMMENT ON COLUMN bids.status IS 'Status: SUBMITTED, ACCEPTED, REJECTED';
