import re
import sys

with open('/home/huji/blps/docs/my_diagram_test_comunda_modelv2.bpmn', 'r') as f:
    xml = f.read()

# ---------------------------------------------------------------
# 1. Set isExecutable="true" on both <bpmn:process> elements
# ---------------------------------------------------------------
xml = xml.replace('isExecutable="false"', 'isExecutable="true"')

# ---------------------------------------------------------------
# 2. Add camunda:historyTimeToLive="365" to both process elements
# ---------------------------------------------------------------
xml = xml.replace(
    '<bpmn:process id="Process_1hxi6lx" isExecutable="true">',
    '<bpmn:process id="Process_1hxi6lx" isExecutable="true" camunda:historyTimeToLive="365">'
)
xml = xml.replace(
    '<bpmn:process id="Process_1r3ex56" isExecutable="true">',
    '<bpmn:process id="Process_1r3ex56" isExecutable="true" camunda:historyTimeToLive="365">'
)

# ---------------------------------------------------------------
# 3. Add a start event to BLPS process (Process_1hxi6lx)
# ---------------------------------------------------------------
start_event_blps = '''    <bpmn:startEvent id="StartEvent_blps" name="Start BLPS Process">
      <bpmn:outgoing>Flow_start_to_1o7gu65</bpmn:outgoing>
    </bpmn:startEvent>
    '''
xml = xml.replace(
    '    </bpmn:laneSet>',
    '    </bpmn:laneSet>\n' + start_event_blps
)
# Add the sequence flow for this start event
flow_start = '    <bpmn:sequenceFlow id="Flow_start_to_1o7gu65" sourceRef="StartEvent_blps" targetRef="Activity_1o7gu65" />\n'
xml = xml.replace(
    '    <bpmn:sequenceFlow id="Flow_02txbwn" sourceRef="Activity_1o7gu65" targetRef="Activity_0skapo9" />',
    flow_start + '    <bpmn:sequenceFlow id="Flow_02txbwn" sourceRef="Activity_1o7gu65" targetRef="Activity_0skapo9" />'
)

# ---------------------------------------------------------------
# 4. Fix empty delegateExpression on Activity_0skapo9
# ---------------------------------------------------------------
xml = xml.replace('camunda:delegateExpression=""', 'camunda:delegateExpression="${genericDelegate}"')

# ---------------------------------------------------------------
# 5. Add camunda:delegateExpression="${genericDelegate}" to all
#    serviceTask elements that lack it
# ---------------------------------------------------------------
service_task_ids = [
    'Activity_0bouvmb', 'Activity_1t92751', 'Activity_1nopj0o',
    'Activity_0ac2p0s', 'Activity_0x9syab', 'Activity_0zdzlqs',
    'Activity_0m5wboq', 'Activity_010ctpc', 'Activity_1xnsi5c',
    'Activity_1p4uo32', 'Activity_1mb2kob', 'Activity_1l6y3ow',
    'Activity_03jqbqr', 'Activity_1pkz8se', 'Activity_0fwbwvv',
]
for sid in service_task_ids:
    pattern = '<bpmn:serviceTask id="' + sid + '"'
    idx = xml.find(pattern)
    if idx >= 0:
        end_open = xml.index('>', idx)
        open_tag = xml[idx:end_open + 1]
        if 'camunda:delegateExpression' not in open_tag:
            if open_tag.strip().endswith('/>'):
                xml = xml[:end_open - 1] + ' camunda:delegateExpression="${genericDelegate}"' + xml[end_open - 1:]
            else:
                xml = xml[:end_open] + ' camunda:delegateExpression="${genericDelegate}"' + xml[end_open:]

# ---------------------------------------------------------------
# 6. Convert ALL bpmn:task elements to bpmn:serviceTask
#    (change open tag AND close tag, add delegateExpression)
# ---------------------------------------------------------------
def convert_task_tag(m):
    tag = m.group(0)
    new_tag = tag.replace('<bpmn:task', '<bpmn:serviceTask', 1)
    if 'camunda:delegateExpression' not in new_tag:
        stripped = new_tag.rstrip()
        if stripped.endswith('/>'):
            new_tag = stripped[:-2] + ' camunda:delegateExpression="${genericDelegate}" />'
        elif stripped.endswith('>'):
            new_tag = stripped[:-1] + ' camunda:delegateExpression="${genericDelegate}">'
    return new_tag

xml = re.sub(r'<bpmn:task[^>]*>', convert_task_tag, xml)
xml = xml.replace('</bpmn:task>', '</bpmn:serviceTask>')

# ---------------------------------------------------------------
# 7. Add errorRef to errorEventDefinitions + corresponding error elements
# ---------------------------------------------------------------
error_ids = list(dict.fromkeys(
    re.findall(r'<bpmn:errorEventDefinition id="ErrorEventDefinition_([^"]+)"', xml)
))

for eid in error_ids:
    old = f'<bpmn:errorEventDefinition id="ErrorEventDefinition_{eid}" />'
    new = f'<bpmn:errorEventDefinition id="ErrorEventDefinition_{eid}" errorRef="Error_{eid}" />'
    # In case it already has other attributes (shouldn't happen but be safe)
    if old not in xml:
        # Try to find it with a more flexible match
        pass
    xml = xml.replace(old, new)

# Collect unique error IDs (already done above) and add error elements before collaboration
for eid in error_ids:
    error_el = f'  <bpmn:error id="Error_{eid}" name="Error_{eid}" errorCode="Error_{eid}" />\n'
    xml = xml.replace(
        '  <bpmn:collaboration id="Collaboration_0dagtvp">',
        error_el + '  <bpmn:collaboration id="Collaboration_0dagtvp">'
    )

# ---------------------------------------------------------------
# 8. Add messageRef to messageEventDefinitions + message elements
#    (none exist in the file, but handle if they did)
# ---------------------------------------------------------------
message_ids = list(dict.fromkeys(
    re.findall(r'<bpmn:messageEventDefinition id="([^"]+)"', xml)
))
for mid in message_ids:
    old = f'<bpmn:messageEventDefinition id="{mid}" />'
    new = f'<bpmn:messageEventDefinition id="{mid}" messageRef="Message_{mid}" />'
    xml = xml.replace(old, new)
    msg_el = f'  <bpmn:message id="Message_{mid}" name="Message_{mid}" />\n'
    xml = xml.replace(
        '  <bpmn:collaboration id="Collaboration_0dagtvp">',
        msg_el + '  <bpmn:collaboration id="Collaboration_0dagtvp">'
    )

# ---------------------------------------------------------------
# 9. Fix exclusive gateways – add default + conditionExpression
# ---------------------------------------------------------------
# Map: gateway_id -> (default_flow, condition_flow)
# For gatways with "Нет" flow, default = "Нет" flow, condition on "Да".
# For those without named "Нет" (Gateway_0fykta9), first child = default,
# condition on second.
gateway_flows = {
    'Gateway_1utvqct': ('Flow_10turtq', 'Flow_1315y0g'),
    'Gateway_1epvrjh': ('Flow_08067wo', 'Flow_0p0vwwu'),
    'Gateway_0kw50os': ('Flow_184v44u', 'Flow_0umu97f'),
    'Gateway_0fykta9': ('Flow_1h288zu', 'Flow_1kjgb8r'),
    'Gateway_17rv298': ('Flow_0lrndda', 'Flow_02cst0v'),
    'Gateway_0hjckgo': ('Flow_0amn5ug', 'Flow_1v4mnmh'),
    'Gateway_07l3i8f': ('Flow_1mzirph', 'Flow_1rxhfff'),
}

for gw_id, (default_flow, condition_flow) in gateway_flows.items():
    # Add default attribute to gateway if not present
    gw_pattern = f'<bpmn:exclusiveGateway id="{gw_id}"'
    idx = xml.find(gw_pattern)
    if idx >= 0:
        end_open = xml.index('>', idx)
        tag = xml[idx:end_open]
        if 'default=' not in tag:
            xml = xml[:end_open] + f' default="{default_flow}"' + xml[end_open:]

    # Add conditionExpression to the condition flow (convert self-closing to
    # container element if needed)
    sf_pattern = f'<bpmn:sequenceFlow id="{condition_flow}"'
    sf_idx = xml.find(sf_pattern)
    if sf_idx >= 0:
        sf_end = xml.index('/>', sf_idx)
        chunk = xml[sf_idx:sf_end + 2]
        if '<bpmn:conditionExpression' not in chunk:
            condition_xml = (
                f'{chunk[:-2]}>\n'
                f'      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${{true}}</bpmn:conditionExpression>\n'
                f'    </bpmn:sequenceFlow>'
            )
            xml = xml[:sf_idx] + condition_xml + xml[sf_end + 2:]

# ---------------------------------------------------------------
# 10. Add start event inside each transaction subprocess
# ---------------------------------------------------------------
transaction_start_tasks = [
    ('Activity_09l2c40', 'Activity_0x9syab'),
    ('Activity_1yr9qgp', 'Activity_0m5wboq'),
    ('Activity_15oo7aw', 'Activity_1p4uo32'),
]

for txn_id, first_task_id in transaction_start_tasks:
    # Insert start event before the first serviceTask
    task_pattern = f'<bpmn:serviceTask id="{first_task_id}"'
    task_idx = xml.find(task_pattern)
    if task_idx >= 0:
        start_id = f'StartEvent_{txn_id}'
        flow_id = f'Flow_{start_id}_to_{first_task_id}'
        start_xml = (
            f'      <bpmn:startEvent id="{start_id}">\n'
            f'        <bpmn:outgoing>{flow_id}</bpmn:outgoing>\n'
            f'      </bpmn:startEvent>\n'
            f'      <bpmn:sequenceFlow id="{flow_id}" sourceRef="{start_id}" targetRef="{first_task_id}" />\n'
        )
        xml = xml[:task_idx] + start_xml + xml[task_idx:]

# ---------------------------------------------------------------
# 11. Write patched file
# ---------------------------------------------------------------
with open('/home/huji/blps/docs/my_diagram_test_comunda_modelv2_patched.bpmn', 'w') as f:
    f.write(xml)

# ---------------------------------------------------------------
# Validate with ElementTree
# ---------------------------------------------------------------
try:
    from xml.etree.ElementTree import parse, ParseError
    import io
    parse(io.StringIO(xml))
    print("VALIDATION PASSED: XML parsed successfully")
    print(f"File written to: /home/huji/blps/docs/my_diagram_test_comunda_modelv2_patched.bpmn")
except ParseError as e:
    print(f"VALIDATION FAILED: {e}")
    sys.exit(1)
