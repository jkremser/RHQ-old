if (action.name == 'echo') {
    echo();
}

function echo() {
    msg = parameters.getSimple('msg').stringValue;
    return org.rhq.core.pluginapi.operation.OperationResult(msg);
}