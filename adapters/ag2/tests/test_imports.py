import pytest

def test_module_imports():
    """Verify that all core modules can be imported without syntax or dependency errors."""
    try:
        import antimatter_bridge.server
        import antimatter_bridge.agent_bridge
        import antimatter_bridge.cli
    except ImportError as e:
        pytest.fail(f"Failed to import a core module: {e}")
