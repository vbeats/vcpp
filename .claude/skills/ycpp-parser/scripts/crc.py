"""CRC-16/Modbus compatible with the project's Java implementation."""


def crc16_modbus(data: bytes, initial: int = 0xFFFF) -> int:
    """Return the conventional Modbus numeric CRC."""
    crc = initial & 0xFFFF
    for byte in data:
        crc ^= byte
        for _ in range(8):
            crc = (crc >> 1) ^ 0xA001 if crc & 1 else crc >> 1
    return crc & 0xFFFF


def crc_wire_bytes(data: bytes) -> bytes:
    """Return the CRC in protocol order: low byte followed by high byte."""
    value = crc16_modbus(data)
    return bytes((value & 0xFF, (value >> 8) & 0xFF))


def crc_report(data: bytes, transmitted: bytes) -> dict:
    """Build a stable, human- and machine-readable CRC result."""
    computed = crc_wire_bytes(data)
    return {
        "algorithm": "CRC-16/Modbus",
        "transmitted_hex": transmitted.hex().upper(),
        "computed_hex": computed.hex().upper(),
        "transmitted_value": int.from_bytes(transmitted, "big") if len(transmitted) == 2 else None,
        "computed_value": int.from_bytes(computed, "big"),
        "match": transmitted == computed,
    }
