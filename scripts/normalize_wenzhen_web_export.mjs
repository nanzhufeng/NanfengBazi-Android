#!/usr/bin/env node

import fs from "node:fs";

const [groupsPath, usersPath, notesPath, celebritiesPath, outputPath] = process.argv.slice(2);
if (![groupsPath, usersPath, notesPath, celebritiesPath, outputPath].every(Boolean)) {
  throw new Error(
    "usage: normalize_wenzhen_web_export.mjs groups.json users.json notes.jsonl celebrities.txt output.json",
  );
}

const groupsEnvelope = JSON.parse(fs.readFileSync(groupsPath, "utf8"));
const usersEnvelope = JSON.parse(fs.readFileSync(usersPath, "utf8"));
const groupNamesById = new Map(groupsEnvelope.data.items.map((item) => [item.id, item.name.trim()]));
const notesByCaseId = new Map(
  fs.readFileSync(notesPath, "utf8")
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => JSON.parse(line))
    .map((entry) => [entry.caseGuid, entry.body?.data ?? null]),
);

const parseJsonString = (value, fallback) => {
  if (!value) return fallback;
  try {
    return JSON.parse(value);
  } catch {
    return fallback;
  }
};

const parseDateTime = (value) => {
  const match = String(value ?? "").trim().match(
    /^(\d{4})-(\d{1,2})-(\d{1,2})[ T](\d{1,2}):(\d{1,2})(?::(\d{1,2}))?$/,
  );
  if (!match) throw new Error(`invalid date time: ${value}`);
  return {
    year: Number(match[1]),
    month: Number(match[2]),
    day: Number(match[3]),
    hour: Number(match[4]),
    minute: Number(match[5]),
    second: Number(match[6] ?? 0),
  };
};

const parsePillars = (value) => {
  const chars = String(value ?? "").replace(/\s+/g, "");
  if ([...chars].length !== 8) throw new Error(`invalid four pillars: ${value}`);
  const parts = [...chars];
  return {
    year: parts.slice(0, 2).join(""),
    month: parts.slice(2, 4).join(""),
    day: parts.slice(4, 6).join(""),
    hour: parts.slice(6, 8).join(""),
  };
};

const parseFeedbackPayload = (value) => {
  const parts = String(value ?? "").split("|");
  const jxDex = parts.pop() ?? "";
  const hasStatus = parts.pop() ?? "";
  return {
    content: parts.join("|").trim(),
    status: hasStatus === "1" ? ({ 0: "平", 1: "吉", 2: "凶" }[jxDex] ?? "") : "",
  };
};

const parseTimelineName = (value) => {
  const sourceLabel = String(value ?? "").trim();
  const match = sourceLabel.match(/(\d{4}|\d{2}\*)年\s*([^\s大运]+)/);
  if (!match) throw new Error(`invalid timeline name: ${value}`);
  return {
    year: match[1].includes("*") ? Number(`${match[1].slice(0, 2)}00`) : Number(match[1]),
    stemBranch: match[2],
    sourceLabel,
  };
};

const normalizeNotes = (raw) => {
  const feedback = parseJsonString(raw?.feedbackData, []);
  const profile = { occupation: "", education: "", finance: "", marriage: "", health: "" };
  const profileNames = new Map([
    ["职业", "occupation"],
    ["学历", "education"],
    ["财富", "finance"],
    ["婚姻", "marriage"],
    ["健康状态", "health"],
  ]);
  const extraFeedback = [];
  const timeline = [];
  let order = 0;
  feedback.forEach((item, itemIndex) => {
    if (item.type === 0 || item.type === 1) {
      const value = String(item.data ?? "").trim();
      const field = profileNames.get(item.name);
      if (field) profile[field] = value;
      else if (value) extraFeedback.push(`${item.name}：${value}`);
      return;
    }
    if (item.type !== 2) return;
    const isDecade = String(item.name ?? "").includes("大运");
    if (!isDecade) {
      const label = parseTimelineName(item.name);
      const payload = parseFeedbackPayload(item.data);
      timeline.push({
        sourceId: `item-${itemIndex}`,
        level: "ANNUAL",
        ...label,
        ...payload,
        order: order++,
      });
      return;
    }
    const decadeLabel = parseTimelineName(item.name);
    const decadeData = parseJsonString(item.data, { data: item.data, list: [] });
    const decadePayload = parseFeedbackPayload(decadeData.data);
    timeline.push({
      sourceId: `item-${itemIndex}`,
      level: "DECADE",
      ...decadeLabel,
      ...decadePayload,
      order: order++,
    });
    (decadeData.list ?? []).forEach((annual, annualIndex) => {
      const label = parseTimelineName(annual.name);
      const payload = parseFeedbackPayload(annual.data);
      timeline.push({
        sourceId: `item-${itemIndex}-annual-${annualIndex}`,
        level: "ANNUAL",
        ...label,
        ...payload,
        order: order++,
      });
    });
  });
  return {
    profile,
    ownerFeedback: extraFeedback.join("\n\n"),
    masterCommentary: String(raw?.content ?? "").trim(),
    timeline,
  };
};

const sourceUsers = usersEnvelope.data.items.flatMap((group) => group.userList.items);
const uniqueUsers = [...new Map(sourceUsers.map((item) => [item.guid, item])).values()];
const userCases = uniqueUsers.map((item) => ({
  sourceId: item.guid,
  name: String(item.name ?? "").trim(),
  sex: Number(item.sex) === 1 ? "男" : "女",
  groupName: groupNamesById.get(item.groupGuid) ?? null,
  originalSolarTime: parseDateTime(item.solarTime),
  adoptedSourceTime: parseDateTime(item.sunTime || item.solarTime),
  location: String(item.location ?? "").trim(),
  fourPillars: parsePillars(item.bz),
  ...normalizeNotes(notesByCaseId.get(item.guid)),
}));

const celebrityGroupNames = {
  mr0: "君主",
  mr1: "历史名人",
  mr2: "商界",
  mr3: "娱乐",
  mr4: "文学",
  mr5: "僧道",
  mr6: "国外",
};
const celebrityCases = fs.readFileSync(celebritiesPath, "utf8")
  .split(/\r?\n/)
  .filter(Boolean)
  .map((line, index) => {
    const fields = line.split(",");
    if (fields.length !== 8) throw new Error(`invalid celebrity row ${index + 1}`);
    const [name, sex, pillars, solarTime, groupId, isLiuNian, periodTag, identityTag] = fields;
    const groupName = celebrityGroupNames[groupId];
    if (!groupName) throw new Error(`invalid celebrity group ${groupId}`);
    return {
      sourceId: `${groupId}:${index}:${name}:${solarTime}`,
      name: name.trim(),
      sex: sex.trim(),
      groupName,
      solarTime: parseDateTime(solarTime),
      fourPillars: parsePillars(pillars),
      periodTag: periodTag.trim(),
      identityTag: identityTag.trim(),
      sourceIsLiuNian: Number(isLiuNian),
    };
  })
  .map(({ sourceIsLiuNian: _ignored, ...item }) => item);

const output = {
  format: "nanfeng-bazi-wenzhen-web-import",
  version: 1,
  extractedAt: new Date().toISOString(),
  userGroups: [...groupNamesById.values()],
  celebrityGroups: Object.values(celebrityGroupNames),
  userCases,
  celebrityCases,
};

if (userCases.length !== 830 || celebrityCases.length !== 351) {
  throw new Error(`unexpected counts: users=${userCases.length}, celebrities=${celebrityCases.length}`);
}
fs.writeFileSync(outputPath, JSON.stringify(output), { mode: 0o600 });
